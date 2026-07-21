package com.gst_reconsilation.gstr1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gst_reconsilation.gstr1.dto.Gstr1SubmitCredentials;
import com.gst_reconsilation.gstr1.dto.Gstr1SubmitResult;
import com.gst_reconsilation.gstr1.dto.api.WhitebooksGstr1Payload;
import com.gst_reconsilation.gstr1.dto.api.WhitebooksGstr1Payload.*;
import com.gst_reconsilation.gstr1.entity.*;
import com.gst_reconsilation.gstr1.repository.Gstr1FilingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maps the already-uploaded GSTR-1 filing (via {@link Gstr1UploadService}) into
 * the Whitebooks submission payload shape and calls PUT /gstr1/retsave.
 *
 * ASSUMPTIONS / GAPS (flagged inline too):
 *  - gt / cur_gt (gross turnover) aren't tracked anywhere in the schema - supplied
 *    via {@link Gstr1SubmitCredentials} at submit time, defaulting to zero.
 *  - IGST/CGST/SGST splits are computed as taxableValue x rate, divided intra/inter
 *    by comparing each row's place-of-supply state code to the company's home
 *    state code (first 2 digits of the GSTIN) - same approach used elsewhere
 *    (Gstr1ReportService, Gstr3bPreviewService).
 *  - "nil" (table 8) has no B2B/B2C x inter/intra classification in Gstr1Exemp,
 *    so all rows are aggregated into a single row tagged sply_ty="INTRB2B" -
 *    almost certainly wrong for a real filing, needs a real classification
 *    column before this goes near production.
 *  - doc_issue's numeric doc_num code is guessed from natureOfDocument via
 *    keyword matching (Gstr1Docs has no numeric code column) - verify the
 *    mapping table below against Whitebooks' actual doc_num enum.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Gstr1SubmitService {

    private final Gstr1FilingRepository filingRepository;
    private final Gstr1UploadService uploadService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gstr1.api.submit-base-url:https://apisandbox.whitebooks.in}")
    private String baseUrl;

    private static final DateTimeFormatter ENTITY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /** Builds the exact payload that would be submitted, without calling the API. */
    public WhitebooksGstr1Payload buildSubmitPayload(Integer filingId, BigDecimal grossTurnover, BigDecimal currentGrossTurnover) {
        Gstr1Filing filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Gstr1Filing not found: " + filingId));

        String homeState = stateCode(filing.getCompanyGST() != null ? filing.getCompanyGST().getGstNumber() : null);

        WhitebooksGstr1Payload payload = new WhitebooksGstr1Payload();
        payload.setGstin(filing.getCompanyGST() != null ? filing.getCompanyGST().getGstNumber() : null);
        payload.setFp(filing.getTaxPeriod());
        payload.setGt(nz(grossTurnover));
        payload.setCurGt(nz(currentGrossTurnover));

        payload.setB2b(buildB2b(filingId, homeState));
        payload.setB2ba(buildB2ba(filingId, homeState));
        payload.setB2cl(buildB2cl(filingId));
        payload.setB2cla(buildB2cla(filingId));
        payload.setCdnr(buildCdnr(filingId, homeState));
        payload.setCdnra(buildCdnra(filingId, homeState));
        payload.setB2cs(buildB2cs(filingId, homeState));
        payload.setB2csa(buildB2csa(filingId, homeState));
        payload.setExp(buildExp(filingId));
        payload.setExpa(buildExpa(filingId));
        payload.setHsn(buildHsn(filingId));
        payload.setNil(buildNil(filingId));
        payload.setAt(buildAdvance(uploadService.getAt(filingId), Gstr1At::getPlaceOfSupply, Gstr1At::getRate,
                Gstr1At::getGrossAdvanceReceived, Gstr1At::getCessAmount, homeState));
        payload.setAta(buildAdvanceAmend(uploadService.getAta(filingId), Gstr1Ata::getOriginalPlaceOfSupply,
                Gstr1Ata::getOriginalMonth, Gstr1Ata::getRate, Gstr1Ata::getGrossAdvanceReceived,
                Gstr1Ata::getCessAmount, homeState));
        payload.setTxpd(buildAdvance(uploadService.getAtadj(filingId), Gstr1Atadj::getPlaceOfSupply, Gstr1Atadj::getRate,
                Gstr1Atadj::getGrossAdvanceAdjusted, Gstr1Atadj::getCessAmount, homeState));
        payload.setTxpda(buildAdvanceAmend(uploadService.getAtadja(filingId), Gstr1Atadja::getOriginalPlaceOfSupply,
                Gstr1Atadja::getOriginalMonth, Gstr1Atadja::getRate, Gstr1Atadja::getGrossAdvanceAdjusted,
                Gstr1Atadja::getCessAmount, homeState));
        payload.setDocIssue(buildDocIssue(filingId));
        payload.setCdnur(buildCdnur(filingId));
        payload.setCdnura(buildCdnura(filingId));

        return payload;
    }

    @Transactional
    public Gstr1SubmitResult submit(Integer filingId, Gstr1SubmitCredentials creds, Integer userId) {
        Gstr1Filing filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Gstr1Filing not found: " + filingId));

        WhitebooksGstr1Payload payload = buildSubmitPayload(filingId, creds.getGrossTurnover(), creds.getCurrentGrossTurnover());
        payload.setGstin(filing.getCompanyGST() != null ? filing.getCompanyGST().getGstNumber() : creds.getGstin());
        payload.setFp(filing.getTaxPeriod());

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/gstr1/retsave")
                .queryParam("email", creds.getEmail())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("gstin", creds.getGstin());
        headers.set("ret_period", creds.getRetPeriod());
        headers.set("gst_username", creds.getGstUsername());
        headers.set("state_cd", creds.getStateCd());
        headers.set("ip_address", creds.getIpAddress());
        headers.set("txn", creds.getTxn());
        headers.set("client_id", creds.getClientId());
        headers.set("client_secret", creds.getClientSecret());

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.PUT, new HttpEntity<>(payload, headers), String.class);
            String body = resp.getBody();
            String arn = extractArn(body);

            filing.setFilingStatus("FILED");
            filing.setArn(arn);
            filing.setFiledAt(LocalDateTime.now());
            filing.setSubmitResponseRaw(body);
            filing.setUpdatedBy(userId);
            filing.setUpdatedDate(LocalDate.now());
            filingRepository.save(filing);

            log.info("Gstr1Filing {} submitted successfully, arn={}", filingId, arn);
            return Gstr1SubmitResult.builder().success(true).httpStatus(resp.getStatusCode().value())
                    .message("Filed successfully").arn(arn).rawResponse(body).build();

        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            filing.setSubmitResponseRaw(body);
            filing.setUpdatedBy(userId);
            filing.setUpdatedDate(LocalDate.now());
            filingRepository.save(filing);

            log.error("Gstr1Filing {} submission failed: {} - {}", filingId, e.getStatusCode(), body);
            return Gstr1SubmitResult.builder().success(false).httpStatus(e.getStatusCode().value())
                    .message("Submission failed").rawResponse(body).build();
        }
    }

    private String extractArn(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(body);
            for (String key : List.of("arn", "ARN", "ack_num", "ackNum", "acknowledgementNumber", "reference_id")) {
                if (node.has(key)) return node.get(key).asText();
            }
            if (node.has("data") && node.get("data").isObject()) {
                for (String key : List.of("arn", "ARN", "ack_num", "ackNum")) {
                    if (node.get("data").has(key)) return node.get("data").get(key).asText();
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse submit response as JSON to extract ARN: {}", e.getMessage());
        }
        return null;
    }

    // ── B2B / B2BA ───────────────────────────────────────────────
    private List<B2bEntry> buildB2b(Integer filingId, String homeState) {
        Map<String, Map<String, List<Gstr1B2b>>> grouped = group2(uploadService.getB2b(filingId),
                Gstr1B2b::getGstinOfRecipient, Gstr1B2b::getInvoiceNumber);
        List<B2bEntry> result = new ArrayList<>();
        for (var ctinGroup : grouped.entrySet()) {
            B2bEntry entry = new B2bEntry();
            entry.setCtin(ctinGroup.getKey());
            List<B2bInvoice> invoices = new ArrayList<>();
            for (var invGroup : ctinGroup.getValue().entrySet()) {
                List<Gstr1B2b> rows = invGroup.getValue();
                Gstr1B2b first = rows.get(0);
                B2bInvoice inv = new B2bInvoice();
                inv.setInum(first.getInvoiceNumber());
                inv.setIdt(dateStr(first.getInvoiceDate()));
                inv.setVal(first.getInvoiceValue());
                inv.setPos(first.getPlaceOfSupply());
                inv.setRchrg(first.getReverseCharge());
                inv.setEtin(first.getEcommerceGstin());
                inv.setInvTyp(first.getInvoiceType());
                inv.setItms(itemsFrom(rows, r -> r.getRate(), r -> r.getTaxableValue(), r -> r.getCessAmount(),
                        r -> r.getPlaceOfSupply(), homeState));
                invoices.add(inv);
            }
            entry.setInv(invoices);
            result.add(entry);
        }
        return result;
    }

    private List<B2baEntry> buildB2ba(Integer filingId, String homeState) {
        Map<String, Map<String, List<Gstr1B2ba>>> grouped = group2(uploadService.getB2ba(filingId),
                Gstr1B2ba::getGstinOfRecipient, Gstr1B2ba::getRevisedInvoiceNumber);
        List<B2baEntry> result = new ArrayList<>();
        for (var ctinGroup : grouped.entrySet()) {
            B2baEntry entry = new B2baEntry();
            entry.setCtin(ctinGroup.getKey());
            List<B2baInvoice> invoices = new ArrayList<>();
            for (var invGroup : ctinGroup.getValue().entrySet()) {
                List<Gstr1B2ba> rows = invGroup.getValue();
                Gstr1B2ba first = rows.get(0);
                B2baInvoice inv = new B2baInvoice();
                inv.setOinum(first.getOriginalInvoiceNumber());
                inv.setOidt(dateStr(first.getOriginalInvoiceDate()));
                inv.setInum(first.getRevisedInvoiceNumber());
                inv.setIdt(dateStr(first.getRevisedInvoiceDate()));
                inv.setVal(first.getInvoiceValue());
                inv.setPos(first.getPlaceOfSupply());
                inv.setRchrg(first.getReverseCharge());
                inv.setEtin(first.getEcommerceGstin());
                inv.setInvTyp(first.getInvoiceType());
                inv.setItms(itemsFrom(rows, r -> r.getRate(), r -> r.getTaxableValue(), r -> r.getCessAmount(),
                        r -> r.getPlaceOfSupply(), homeState));
                invoices.add(inv);
            }
            entry.setInv(invoices);
            result.add(entry);
        }
        return result;
    }

    // ── B2CL / B2CLA ─────────────────────────────────────────────
    private List<B2clEntry> buildB2cl(Integer filingId) {
        Map<String, Map<String, List<Gstr1B2cl>>> grouped = group2(uploadService.getB2cl(filingId),
                Gstr1B2cl::getPlaceOfSupply, Gstr1B2cl::getInvoiceNumber);
        List<B2clEntry> result = new ArrayList<>();
        for (var posGroup : grouped.entrySet()) {
            B2clEntry entry = new B2clEntry();
            entry.setPos(posGroup.getKey());
            List<B2clInvoice> invoices = new ArrayList<>();
            for (var invGroup : posGroup.getValue().entrySet()) {
                List<Gstr1B2cl> rows = invGroup.getValue();
                Gstr1B2cl first = rows.get(0);
                B2clInvoice inv = new B2clInvoice();
                inv.setInum(first.getInvoiceNumber());
                inv.setIdt(dateStr(first.getInvoiceDate()));
                inv.setVal(first.getInvoiceValue());
                inv.setEtin(first.getEcommerceGstin());
                // B2CL is always inter-state by definition - IGST only, no home-state split needed.
                inv.setItms(itemsFrom(rows, r -> r.getRate(), r -> r.getTaxableValue(), r -> r.getCessAmount(),
                        r -> null, null));
                invoices.add(inv);
            }
            entry.setInv(invoices);
            result.add(entry);
        }
        return result;
    }

    private List<B2claEntry> buildB2cla(Integer filingId) {
        Map<String, Map<String, List<Gstr1B2cla>>> grouped = group2(uploadService.getB2cla(filingId),
                Gstr1B2cla::getOriginalPlaceOfSupply, Gstr1B2cla::getRevisedInvoiceNumber);
        List<B2claEntry> result = new ArrayList<>();
        for (var posGroup : grouped.entrySet()) {
            B2claEntry entry = new B2claEntry();
            entry.setPos(posGroup.getKey());
            List<B2claInvoice> invoices = new ArrayList<>();
            for (var invGroup : posGroup.getValue().entrySet()) {
                List<Gstr1B2cla> rows = invGroup.getValue();
                Gstr1B2cla first = rows.get(0);
                B2claInvoice inv = new B2claInvoice();
                inv.setOinum(first.getOriginalInvoiceNumber());
                inv.setOidt(dateStr(first.getOriginalInvoiceDate()));
                inv.setInum(first.getRevisedInvoiceNumber());
                inv.setIdt(dateStr(first.getRevisedInvoiceDate()));
                inv.setVal(first.getInvoiceValue());
                inv.setEtin(first.getEcommerceGstin());
                inv.setItms(itemsFrom(rows, r -> r.getRate(), r -> r.getTaxableValue(), r -> r.getCessAmount(),
                        r -> null, null));
                invoices.add(inv);
            }
            entry.setInv(invoices);
            result.add(entry);
        }
        return result;
    }

    // ── CDNR / CDNRA ─────────────────────────────────────────────
    private List<CdnrEntry> buildCdnr(Integer filingId, String homeState) {
        Map<String, Map<String, List<Gstr1Cdnr>>> grouped = group2(uploadService.getCdnr(filingId),
                Gstr1Cdnr::getGstinOfRecipient, Gstr1Cdnr::getNoteNumber);
        List<CdnrEntry> result = new ArrayList<>();
        for (var ctinGroup : grouped.entrySet()) {
            CdnrEntry entry = new CdnrEntry();
            entry.setCtin(ctinGroup.getKey());
            List<CdnrNote> notes = new ArrayList<>();
            for (var noteGroup : ctinGroup.getValue().entrySet()) {
                List<Gstr1Cdnr> rows = noteGroup.getValue();
                Gstr1Cdnr first = rows.get(0);
                CdnrNote note = new CdnrNote();
                note.setNtty(first.getNoteType());
                note.setNtNum(first.getNoteNumber());
                note.setNtDt(dateStr(first.getNoteDate()));
                note.setPGst("N");
                note.setPos(first.getPlaceOfSupply());
                note.setRchrg(first.getReverseCharge());
                note.setInvTyp(first.getNoteSupplyType());
                note.setVal(first.getNoteValue());
                note.setItms(itemsFrom(rows, r -> r.getRate(), r -> r.getTaxableValue(), r -> r.getCessAmount(),
                        r -> r.getPlaceOfSupply(), homeState));
                notes.add(note);
            }
            entry.setNt(notes);
            result.add(entry);
        }
        return result;
    }

    private List<CdnraEntry> buildCdnra(Integer filingId, String homeState) {
        Map<String, Map<String, List<Gstr1Cdnra>>> grouped = group2(uploadService.getCdnra(filingId),
                Gstr1Cdnra::getGstinOfRecipient, Gstr1Cdnra::getRevisedNoteNumber);
        List<CdnraEntry> result = new ArrayList<>();
        for (var ctinGroup : grouped.entrySet()) {
            CdnraEntry entry = new CdnraEntry();
            entry.setCtin(ctinGroup.getKey());
            List<CdnraNote> notes = new ArrayList<>();
            for (var noteGroup : ctinGroup.getValue().entrySet()) {
                List<Gstr1Cdnra> rows = noteGroup.getValue();
                Gstr1Cdnra first = rows.get(0);
                CdnraNote note = new CdnraNote();
                note.setNtty(first.getNoteType());
                note.setOntNum(first.getOriginalNoteNumber());
                note.setOntDt(dateStr(first.getOriginalNoteDate()));
                note.setNtNum(first.getRevisedNoteNumber());
                note.setNtDt(dateStr(first.getRevisedNoteDate()));
                note.setPGst("N");
                note.setPos(first.getPlaceOfSupply());
                note.setRchrg(first.getReverseCharge());
                note.setInvTyp(first.getNoteSupplyType());
                note.setVal(first.getNoteValue());
                note.setItms(itemsFrom(rows, r -> r.getRate(), r -> r.getTaxableValue(), r -> r.getCessAmount(),
                        r -> r.getPlaceOfSupply(), homeState));
                notes.add(note);
            }
            entry.setNt(notes);
            result.add(entry);
        }
        return result;
    }

    // ── CDNUR / CDNURA ───────────────────────────────────────────
    private List<CdnurEntry> buildCdnur(Integer filingId) {
        return uploadService.getCdnur(filingId).stream().map(r -> {
            CdnurEntry e = new CdnurEntry();
            e.setTyp(r.getUrType());
            e.setNtty(r.getNoteType());
            e.setNtNum(r.getNoteNumber());
            e.setNtDt(dateStr(r.getNoteDate()));
            e.setPGst("N");
            e.setPos(r.getPlaceOfSupply());
            e.setVal(r.getNoteValue());
            BigDecimal[] tax = taxSplit(r.getTaxableValue(), r.getRate(), null, null); // CDNUR always inter-state
            e.setItms(List.of(item(1, r.getRate(), r.getTaxableValue(), tax[0], tax[1], tax[2], r.getCessAmount())));
            return e;
        }).collect(Collectors.toList());
    }

    private List<CdnuraEntry> buildCdnura(Integer filingId) {
        return uploadService.getCdnura(filingId).stream().map(r -> {
            CdnuraEntry e = new CdnuraEntry();
            e.setOntNum(r.getOriginalNoteNumber());
            e.setOntDt(dateStr(r.getOriginalNoteDate()));
            e.setNtNum(r.getRevisedNoteNumber());
            e.setNtDt(dateStr(r.getRevisedNoteDate()));
            e.setNtty(r.getNoteType());
            e.setTyp(r.getUrType());
            e.setPGst("N");
            e.setVal(r.getNoteValue());
            BigDecimal[] tax = taxSplit(r.getTaxableValue(), r.getRate(), null, null);
            e.setItms(List.of(item(1, r.getRate(), r.getTaxableValue(), tax[0], tax[1], tax[2], r.getCessAmount())));
            return e;
        }).collect(Collectors.toList());
    }

    // ── B2CS / B2CSA ─────────────────────────────────────────────
    private List<B2csRow> buildB2cs(Integer filingId, String homeState) {
        return uploadService.getB2cs(filingId).stream().map(r -> {
            B2csRow row = new B2csRow();
            row.setSplyTy(isIntra(r.getPlaceOfSupply(), homeState) ? "INTRA" : "INTER");
            row.setRt(r.getRate());
            row.setTyp(r.getType());
            row.setEtin(r.getEcommerceGstin());
            row.setPos(r.getPlaceOfSupply());
            row.setTxval(r.getTaxableValue());
            BigDecimal[] tax = taxSplit(r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState);
            row.setIamt(tax[0]);
            row.setCsamt(r.getCessAmount());
            return row;
        }).collect(Collectors.toList());
    }

    private List<B2csaEntry> buildB2csa(Integer filingId, String homeState) {
        return uploadService.getB2csa(filingId).stream().map(r -> {
            B2csaEntry e = new B2csaEntry();
            e.setOmon(r.getOriginalMonth());
            e.setSplyTy(isIntra(r.getPlaceOfSupply(), homeState) ? "INTRA" : "INTER");
            e.setTyp(r.getType());
            e.setEtin(r.getEcommerceGstin());
            e.setPos(r.getPlaceOfSupply());
            BigDecimal[] tax = taxSplit(r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState);
            FlatTaxItem item = new FlatTaxItem();
            item.setRt(r.getRate()); item.setTxval(r.getTaxableValue());
            item.setIamt(tax[0]); item.setCamt(tax[1]); item.setSamt(tax[2]); item.setCsamt(r.getCessAmount());
            e.setItms(List.of(item));
            return e;
        }).collect(Collectors.toList());
    }

    // ── EXP / EXPA ───────────────────────────────────────────────
    private List<ExpEntry> buildExp(Integer filingId) {
        Map<String, List<Gstr1Exp>> grouped = uploadService.getExp(filingId).stream()
                .collect(Collectors.groupingBy(r -> nvl(r.getExportType()), LinkedHashMap::new, Collectors.toList()));
        List<ExpEntry> result = new ArrayList<>();
        for (var g : grouped.entrySet()) {
            ExpEntry e = new ExpEntry();
            e.setExpTyp(g.getKey());
            e.setInv(g.getValue().stream().map(r -> {
                ExpInvoice inv = new ExpInvoice();
                inv.setInum(r.getInvoiceNumber());
                inv.setIdt(dateStr(r.getInvoiceDate()));
                inv.setVal(r.getInvoiceValue());
                inv.setSbpcode(r.getPortCode());
                inv.setSbnum(r.getShippingBillNumber());
                inv.setSbdt(dateStr(r.getShippingBillDate()));
                ExpItem item = new ExpItem();
                item.setTxval(r.getTaxableValue()); item.setRt(r.getRate());
                item.setIamt(taxAmt(r.getTaxableValue(), r.getRate())); item.setCsamt(r.getCessAmount());
                inv.setItms(List.of(item));
                return inv;
            }).collect(Collectors.toList()));
            result.add(e);
        }
        return result;
    }

    private List<ExpaEntry> buildExpa(Integer filingId) {
        Map<String, List<Gstr1Expa>> grouped = uploadService.getExpa(filingId).stream()
                .collect(Collectors.groupingBy(r -> nvl(r.getExportType()), LinkedHashMap::new, Collectors.toList()));
        List<ExpaEntry> result = new ArrayList<>();
        for (var g : grouped.entrySet()) {
            ExpaEntry e = new ExpaEntry();
            e.setExpTyp(g.getKey());
            e.setInv(g.getValue().stream().map(r -> {
                ExpaInvoice inv = new ExpaInvoice();
                inv.setOinum(r.getOriginalInvoiceNumber());
                inv.setOidt(dateStr(r.getOriginalInvoiceDate()));
                inv.setInum(r.getRevisedInvoiceNumber());
                inv.setIdt(dateStr(r.getRevisedInvoiceDate()));
                inv.setVal(r.getInvoiceValue());
                inv.setSbpcode(r.getPortCode());
                inv.setSbnum(r.getShippingBillNumber());
                inv.setSbdt(dateStr(r.getShippingBillDate()));
                ExpItem item = new ExpItem();
                item.setTxval(r.getTaxableValue()); item.setRt(r.getRate());
                item.setIamt(taxAmt(r.getTaxableValue(), r.getRate())); item.setCsamt(r.getCessAmount());
                inv.setItms(List.of(item));
                return inv;
            }).collect(Collectors.toList()));
            result.add(e);
        }
        return result;
    }

    // ── HSN ──────────────────────────────────────────────────────
    private Hsn buildHsn(Integer filingId) {
        List<HsnRow> rows = new ArrayList<>();
        int num = 1;
        for (Gstr1HsnB2b r : uploadService.getHsnB2b(filingId)) {
            rows.add(hsnRow(num++, r.getHsn(), r.getDescription(), r.getUqc(), r.getTotalQuantity(),
                    r.getRate(), r.getTaxableValue(), r.getIntegratedTaxAmount(), r.getCessAmount()));
        }
        for (Gstr1HsnB2c r : uploadService.getHsnB2c(filingId)) {
            rows.add(hsnRow(num++, r.getHsn(), r.getDescription(), r.getUqc(), r.getTotalQuantity(),
                    r.getRate(), r.getTaxableValue(), r.getIntegratedTaxAmount(), r.getCessAmount()));
        }
        Hsn hsn = new Hsn();
        hsn.setData(rows);
        return hsn;
    }

    private HsnRow hsnRow(int num, String hsnSc, String desc, String uqc, BigDecimal qty, BigDecimal rt,
                          BigDecimal txval, BigDecimal iamt, BigDecimal csamt) {
        HsnRow row = new HsnRow();
        row.setNum(num); row.setHsnSc(hsnSc); row.setDesc(desc); row.setUqc(uqc);
        row.setQty(qty); row.setRt(rt); row.setTxval(txval); row.setIamt(iamt); row.setCsamt(csamt);
        return row;
    }

    // ── NIL ──────────────────────────────────────────────────────
    /** TODO: Gstr1Exemp has no B2B/B2C x inter/intra classification, so every
     *  record is collapsed into one row tagged sply_ty="INTRB2B" - this is very
     *  likely wrong for real data and needs a real classification column. */
    private NilBlock buildNil(Integer filingId) {
        BigDecimal expt = BigDecimal.ZERO, nilAmt = BigDecimal.ZERO, ngsup = BigDecimal.ZERO;
        for (Gstr1Exemp r : uploadService.getExemp(filingId)) {
            expt = expt.add(nz(r.getExemptedSupplies()));
            nilAmt = nilAmt.add(nz(r.getNilRatedSupplies()));
            ngsup = ngsup.add(nz(r.getNonGstSupplies()));
        }
        NilRow row = new NilRow();
        row.setSplyTy("INTRB2B");
        row.setExptAmt(expt); row.setNilAmt(nilAmt); row.setNgsupAmt(ngsup);
        NilBlock block = new NilBlock();
        block.setInv(List.of(row));
        return block;
    }

    // ── AT / ATA / TXPD / TXPDA ──────────────────────────────────
    private <T> List<AdvanceEntry> buildAdvance(List<T> rows, Function<T, String> pos, Function<T, BigDecimal> rate,
                                                Function<T, BigDecimal> grossAmt, Function<T, BigDecimal> cess,
                                                String homeState) {
        Map<String, List<T>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> nvl(pos.apply(r)), LinkedHashMap::new, Collectors.toList()));
        List<AdvanceEntry> result = new ArrayList<>();
        for (var g : grouped.entrySet()) {
            AdvanceEntry e = new AdvanceEntry();
            e.setPos(g.getKey());
            e.setSplyTy(isIntra(g.getKey(), homeState) ? "INTRA" : "INTER");
            e.setItms(g.getValue().stream().map(r -> advanceItem(rate.apply(r), grossAmt.apply(r), cess.apply(r),
                    g.getKey(), homeState)).collect(Collectors.toList()));
            result.add(e);
        }
        return result;
    }

    private <T> List<AdvanceAmendEntry> buildAdvanceAmend(List<T> rows, Function<T, String> pos, Function<T, String> omon,
                                                          Function<T, BigDecimal> rate, Function<T, BigDecimal> grossAmt,
                                                          Function<T, BigDecimal> cess, String homeState) {
        Map<String, List<T>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> nvl(omon.apply(r)) + "|" + nvl(pos.apply(r)),
                        LinkedHashMap::new, Collectors.toList()));
        List<AdvanceAmendEntry> result = new ArrayList<>();
        for (var g : grouped.entrySet()) {
            T first = g.getValue().get(0);
            AdvanceAmendEntry e = new AdvanceAmendEntry();
            e.setOmon(omon.apply(first));
            e.setPos(pos.apply(first));
            e.setSplyTy(isIntra(pos.apply(first), homeState) ? "INTRA" : "INTER");
            e.setItms(g.getValue().stream().map(r -> advanceItem(rate.apply(r), grossAmt.apply(r), cess.apply(r),
                    pos.apply(r), homeState)).collect(Collectors.toList()));
            result.add(e);
        }
        return result;
    }

    private AdvanceItem advanceItem(BigDecimal rate, BigDecimal grossAmt, BigDecimal cess, String pos, String homeState) {
        BigDecimal[] tax = taxSplit(grossAmt, rate, pos, homeState);
        AdvanceItem item = new AdvanceItem();
        item.setRt(rate); item.setAdAmt(grossAmt);
        item.setIamt(tax[0]); item.setCamt(tax[1]); item.setSamt(tax[2]); item.setCsamt(nz(cess));
        return item;
    }

    // ── DOC ISSUE ────────────────────────────────────────────────
    /** TODO: doc_num is guessed from natureOfDocument via keyword match - Gstr1Docs
     *  has no numeric code column. Verify against Whitebooks' actual doc_num list. */
    private DocIssue buildDocIssue(Integer filingId) {
        Map<Integer, List<Gstr1Docs>> grouped = uploadService.getDocs(filingId).stream()
                .collect(Collectors.groupingBy(r -> guessDocNum(r.getNatureOfDocument()), LinkedHashMap::new, Collectors.toList()));
        List<DocDet> docDets = new ArrayList<>();
        for (var g : grouped.entrySet()) {
            DocDet det = new DocDet();
            det.setDocNum(g.getKey());
            int idx = 1;
            List<DocDetailRow> rows = new ArrayList<>();
            for (Gstr1Docs d : g.getValue()) {
                DocDetailRow row = new DocDetailRow();
                row.setNum(idx++);
                row.setFrom(d.getSrNoFrom());
                row.setTo(d.getSrNoTo());
                row.setTotnum(d.getTotalNumber());
                row.setCancel(d.getCancelled());
                int total = d.getTotalNumber() != null ? d.getTotalNumber() : 0;
                int cancelled = d.getCancelled() != null ? d.getCancelled() : 0;
                row.setNetIssue(total - cancelled);
                rows.add(row);
            }
            det.setDocs(rows);
            docDets.add(det);
        }
        DocIssue issue = new DocIssue();
        issue.setDocDet(docDets);
        return issue;
    }

    private int guessDocNum(String nature) {
        if (nature == null) return 1;
        String n = nature.toLowerCase();
        if (n.contains("revised")) return 3;
        if (n.contains("debit")) return 4;
        if (n.contains("credit")) return 5;
        if (n.contains("receipt")) return 6;
        if (n.contains("payment voucher")) return 7;
        if (n.contains("refund")) return 8;
        if (n.contains("delivery challan") && n.contains("job")) return 9;
        if (n.contains("delivery challan") && n.contains("approval")) return 10;
        if (n.contains("delivery challan") && n.contains("liquid gas")) return 11;
        if (n.contains("delivery challan")) return 12;
        if (n.contains("unregistered")) return 2;
        return 1; // default: invoices for outward supply
    }

    // ── Generic helpers ──────────────────────────────────────────

    /**
     * Two-level grouping (e.g. supplier GSTIN -> invoice number -> rows sharing that invoice).
     *
     * NOTE: declared to return {@code Map<String, Map<String, List<T>>>} (not the
     * concrete {@code LinkedHashMap<String, LinkedHashMap<String, List<T>>>>} type)
     * because Java generics are invariant: a {@code LinkedHashMap<String, LinkedHashMap<String, List<T>>>}
     * is NOT assignable to a variable declared as {@code Map<String, Map<String, List<T>>>},
     * even though {@code LinkedHashMap} implements {@code Map}. Keeping the declared
     * return type as plain {@code Map}/{@code Map} (while still using LinkedHashMap
     * internally to preserve insertion order) lets every call site keep its natural
     * {@code Map<String, Map<String, List<X>>>} variable declaration without a cast.
     */
    private <T> Map<String, Map<String, List<T>>> group2(
            List<T> rows, Function<T, String> key1, Function<T, String> key2) {
        Map<String, Map<String, List<T>>> map = new LinkedHashMap<>();
        for (T r : rows) {
            map.computeIfAbsent(nvl(key1.apply(r)), k -> new LinkedHashMap<>())
                    .computeIfAbsent(nvl(key2.apply(r)), k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    /** Builds the itms[] array (one {num, itm_det} per row sharing an invoice/note), with tax split. */
    private <T> List<Item> itemsFrom(List<T> rows, Function<T, BigDecimal> rate, Function<T, BigDecimal> txval,
                                     Function<T, BigDecimal> cess, Function<T, String> pos, String homeState) {
        List<Item> items = new ArrayList<>();
        int num = 1;
        for (T r : rows) {
            BigDecimal[] tax = taxSplit(txval.apply(r), rate.apply(r), pos.apply(r), homeState);
            items.add(item(num++, rate.apply(r), txval.apply(r), tax[0], tax[1], tax[2], cess.apply(r)));
        }
        return items;
    }

    private Item item(int num, BigDecimal rate, BigDecimal txval, BigDecimal iamt, BigDecimal camt, BigDecimal samt, BigDecimal csamt) {
        ItemDetail det = new ItemDetail();
        det.setRt(rate); det.setTxval(txval); det.setIamt(iamt); det.setCamt(camt); det.setSamt(samt); det.setCsamt(nz(csamt));
        Item item = new Item();
        item.setNum(num); item.setItmDet(det);
        return item;
    }

    private String stateCode(String posOrGstin) {
        if (posOrGstin == null || posOrGstin.length() < 2) return null;
        return posOrGstin.substring(0, 2);
    }

    private boolean isIntra(String pos, String homeState) {
        return homeState != null && homeState.equals(stateCode(pos));
    }

    private BigDecimal taxAmt(BigDecimal taxableValue, BigDecimal rate) {
        return nz(taxableValue).multiply(nz(rate)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** {igst, cgst, sgst} - if pos/homeState are null (always-inter-state sections like B2CL/exports), everything goes to igst. */
    private BigDecimal[] taxSplit(BigDecimal taxableValue, BigDecimal rate, String pos, String homeState) {
        BigDecimal total = taxAmt(taxableValue, rate);
        if (pos != null && isIntra(pos, homeState)) {
            BigDecimal half = total.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            return new BigDecimal[]{BigDecimal.ZERO, half, half};
        }
        return new BigDecimal[]{total, BigDecimal.ZERO, BigDecimal.ZERO};
    }

    private String dateStr(LocalDate d) { return d == null ? null : d.format(ENTITY_DATE); }
    private String nvl(String s) { return s == null ? "" : s; }
    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}