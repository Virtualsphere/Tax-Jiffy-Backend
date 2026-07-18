package com.gst_reconsilation.gstr3b.service;

import com.gst_reconsilation.gstr3b.dto.ImsCredentials;
import com.gst_reconsilation.gstr3b.dto.api.ImsApiResponse;
import com.gst_reconsilation.gstr3b.dto.api.ImsApiResponse.*;
import com.gst_reconsilation.gstr3b.entity.Gstr3bFiling;
import com.gst_reconsilation.gstr3b.entity.Gstr3bImsInvoice;
import com.gst_reconsilation.gstr3b.repository.Gstr3bFilingRepository;
import com.gst_reconsilation.gstr3b.repository.Gstr3bImsInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls GET /ims/supplierinvoices and persists every invoice/note as a flattened
 * {@link Gstr3bImsInvoice} row, tagged with its IMS action (Accept/Reject/Pending)
 * so the reconciliation step can compare it against the purchase register.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Gstr3bImsSyncService {

    private final Gstr3bFilingRepository filingRepository;
    private final Gstr3bImsInvoiceRepository imsInvoiceRepository;
    private final RestTemplate restTemplate;

    @Value("${gstr3b.api.ims-base-url:https://apisandbox.whitebooks.in}")
    private String baseUrl;

    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Transactional
    public int sync(Integer filingId, ImsCredentials creds, Integer userId) {
        Gstr3bFiling filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Gstr3bFiling not found: " + filingId));

        ImsApiResponse resp = callIms(creds);

        imsInvoiceRepository.deleteByFiling_Id(filingId);

        List<Gstr3bImsInvoice> rows = new ArrayList<>();
        if (resp != null) {
            if (resp.getGstr1() != null) rows.addAll(flatten("GSTR1", resp.getGstr1(), filing, userId));
            if (resp.getGstr1a() != null) rows.addAll(flatten("GSTR1A", resp.getGstr1a(), filing, userId));
        }
        int saved = imsInvoiceRepository.saveAll(rows).size();

        filing.setImsSyncStatus("SYNCED");
        filing.setImsSyncedAt(LocalDateTime.now());
        filingRepository.save(filing);

        log.info("IMS sync complete for Gstr3bFiling {}: {} rows", filingId, saved);
        return saved;
    }

    private ImsApiResponse callIms(ImsCredentials creds) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/ims/supplierinvoices")
                .queryParam("gstin", creds.getGstin())
                .queryParam("email", creds.getEmail())
                .queryParam("retperiod", creds.getRetperiod())
                .queryParam("section", creds.getSection())
                .queryParam("rtnTyp", creds.getRtnTyp())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("get_username", creds.getGstUsername());
        headers.set("state_cd", creds.getStateCd());
        headers.set("ip_address", creds.getIpAddress());
        headers.set("txn", creds.getTxn());
        headers.set("client_id", creds.getClientId());
        headers.set("client_secret", creds.getClientSecret());

        ResponseEntity<ImsApiResponse> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), ImsApiResponse.class);
        return resp.getBody();
    }

    // ── Flattening ───────────────────────────────────────────────

    private List<Gstr3bImsInvoice> flatten(String source, ImsSection section, Gstr3bFiling filing, Integer userId) {
        List<Gstr3bImsInvoice> rows = new ArrayList<>();

        if (section.getB2b() != null) {
            for (ImsB2bEntry entry : section.getB2b()) {
                if (entry.getInv() == null) continue;
                for (ImsB2bInvoice inv : entry.getInv()) {
                    rows.add(rowFromB2b(source, "B2B", filing, userId, entry.getCtin(), inv));
                }
            }
        }
        if (section.getB2ba() != null) {
            for (ImsB2baEntry entry : section.getB2ba()) {
                if (entry.getInv() == null) continue;
                for (ImsB2baInvoice inv : entry.getInv()) {
                    Gstr3bImsInvoice row = rowFromB2b(source, "B2BA", filing, userId, entry.getCtin(), inv);
                    row.setOriginalInvoiceNumber(inv.getOriginalInvoiceNumber());
                    row.setOriginalInvoiceDate(parseDate(inv.getOriginalInvoiceDate()));
                    rows.add(row);
                }
            }
        }
        if (section.getCdnr() != null) {
            for (ImsCdnrEntry entry : section.getCdnr()) {
                if (entry.getNt() == null) continue;
                for (ImsCdnrNote note : entry.getNt()) {
                    rows.add(rowFromCdnr(source, "CDNR", filing, userId, entry.getCtin(), note));
                }
            }
        }
        if (section.getCdnra() != null) {
            for (ImsCdnraEntry entry : section.getCdnra()) {
                if (entry.getNt() == null) continue;
                for (ImsCdnraNote note : entry.getNt()) {
                    Gstr3bImsInvoice row = rowFromCdnr(source, "CDNRA", filing, userId, entry.getCtin(), note);
                    row.setOriginalInvoiceNumber(note.getOriginalNoteNumber());
                    row.setOriginalInvoiceDate(parseDate(note.getOriginalNoteDate()));
                    rows.add(row);
                }
            }
        }
        if (section.getEcom() != null) {
            ImsEcomBlock ecom = section.getEcom();
            if (ecom.getB2b() != null) {
                for (ImsEcomB2bEntry entry : ecom.getB2b()) {
                    if (entry.getInv() == null) continue;
                    for (ImsEcomInvoice inv : entry.getInv()) {
                        rows.add(rowFromEcom(source, "ECOM_B2B", filing, userId, null, entry.getRtin(),
                                entry.getStin(), inv));
                    }
                }
            }
            if (ecom.getUrp2b() != null) {
                for (ImsEcomUrp2bEntry entry : ecom.getUrp2b()) {
                    if (entry.getInv() == null) continue;
                    for (ImsEcomInvoice inv : entry.getInv()) {
                        rows.add(rowFromEcom(source, "ECOM_URP2B", filing, userId, null, entry.getRtin(),
                                null, inv));
                    }
                }
            }
        }
        if (section.getEcoma() != null) {
            ImsEcomaBlock ecoma = section.getEcoma();
            if (ecoma.getB2ba() != null) {
                for (ImsEcomaB2baEntry entry : ecoma.getB2ba()) {
                    if (entry.getInv() == null) continue;
                    for (ImsEcomaInvoice inv : entry.getInv()) {
                        Gstr3bImsInvoice row = rowFromEcom(source, "ECOMA_B2BA", filing, userId, null,
                                entry.getRtin(), entry.getStin(), inv);
                        row.setOriginalInvoiceNumber(inv.getOriginalInvoiceNumber());
                        row.setOriginalInvoiceDate(parseDate(inv.getOriginalInvoiceDate()));
                        rows.add(row);
                    }
                }
            }
            if (ecoma.getUrp2ba() != null) {
                for (ImsEcomaUrp2baEntry entry : ecoma.getUrp2ba()) {
                    if (entry.getInv() == null) continue;
                    for (ImsEcomaInvoice inv : entry.getInv()) {
                        Gstr3bImsInvoice row = rowFromEcom(source, "ECOMA_URP2BA", filing, userId, null,
                                entry.getRtin(), null, inv);
                        row.setOriginalInvoiceNumber(inv.getOriginalInvoiceNumber());
                        row.setOriginalInvoiceDate(parseDate(inv.getOriginalInvoiceDate()));
                        rows.add(row);
                    }
                }
            }
        }
        return rows;
    }

    private Gstr3bImsInvoice rowFromB2b(String source, String sectionName, Gstr3bFiling filing, Integer userId,
                                        String supplierGstin, ImsB2bInvoice inv) {
        BigDecimal[] tax = sumItems(inv.getItms());
        return Gstr3bImsInvoice.builder().filing(filing).createdBy(userId)
                .source(source).section(sectionName).supplierGstin(upper(supplierGstin))
                .invoiceNumber(inv.getInvoiceNumber()).invoiceDate(parseDate(inv.getInvoiceDate()))
                .invoiceValue(inv.getInvoiceValue()).placeOfSupply(inv.getPos())
                .invoiceType(inv.getInvoiceType()).reverseCharge(inv.getReverseCharge())
                .ecommerceGstin(inv.getEtin())
                .taxableValue(tax[0]).integratedTax(tax[1]).centralTax(tax[2]).stateUtTax(tax[3]).cess(tax[4])
                .imsAction(inv.getImsAction()).remarks(inv.getRemarks()).build();
    }

    private Gstr3bImsInvoice rowFromCdnr(String source, String sectionName, Gstr3bFiling filing, Integer userId,
                                         String supplierGstin, ImsCdnrNote note) {
        BigDecimal[] tax = sumItems(note.getItms());
        return Gstr3bImsInvoice.builder().filing(filing).createdBy(userId)
                .source(source).section(sectionName).supplierGstin(upper(supplierGstin))
                .invoiceNumber(note.getNoteNumber()).invoiceDate(parseDate(note.getNoteDate()))
                .invoiceValue(note.getNoteValue()).placeOfSupply(note.getPos())
                .invoiceType(note.getInvoiceType()).reverseCharge(note.getReverseCharge())
                .taxableValue(tax[0]).integratedTax(tax[1]).centralTax(tax[2]).stateUtTax(tax[3]).cess(tax[4])
                .imsAction(note.getImsAction()).remarks(note.getRemarks()).build();
    }

    private Gstr3bImsInvoice rowFromEcom(String source, String sectionName, Gstr3bFiling filing, Integer userId,
                                         String supplierGstin, String recipientGstin, String ecommerceGstin,
                                         ImsEcomInvoice inv) {
        BigDecimal[] tax = sumItems(inv.getItms());
        return Gstr3bImsInvoice.builder().filing(filing).createdBy(userId)
                .source(source).section(sectionName).supplierGstin(upper(supplierGstin))
                .recipientGstin(upper(recipientGstin)).ecommerceGstin(upper(ecommerceGstin))
                .invoiceNumber(inv.getInvoiceNumber()).invoiceDate(parseDate(inv.getInvoiceDate()))
                .invoiceValue(inv.getInvoiceValue()).placeOfSupply(inv.getPos())
                .invoiceType(inv.getInvoiceType())
                .taxableValue(tax[0]).integratedTax(tax[1]).centralTax(tax[2]).stateUtTax(tax[3]).cess(tax[4])
                .imsAction(inv.getImsAction()).remarks(inv.getRemarks()).build();
    }

    /** Sums itms[].itm_det across an invoice/note -> {taxableValue, igst, cgst, sgst, cess}. */
    private BigDecimal[] sumItems(List<ImsItem> items) {
        BigDecimal tv = BigDecimal.ZERO, igst = BigDecimal.ZERO, cgst = BigDecimal.ZERO,
                sgst = BigDecimal.ZERO, cess = BigDecimal.ZERO;
        if (items != null) {
            for (ImsItem item : items) {
                ImsItemDetail d = item.getDetail();
                if (d == null) continue;
                tv = tv.add(nz(d.getTaxableValue()));
                igst = igst.add(nz(d.getIamt()));
                cgst = cgst.add(nz(d.getCamt()));
                sgst = sgst.add(nz(d.getSamt()));
                cess = cess.add(nz(d.getCsamt()));
            }
        }
        return new BigDecimal[]{tv, igst, cgst, sgst, cess};
    }

    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private String upper(String s) { return s != null ? s.toUpperCase() : null; }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s, API_DATE); } catch (Exception e) { return null; }
    }
}