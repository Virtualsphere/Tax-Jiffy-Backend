// ims/service/ImsSyncService.java
package com.gst_reconsilation.ims.service;

import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.config.GstApiCredentialsProperties;
import com.gst_reconsilation.ims.dto.*;
import com.gst_reconsilation.ims.dto.api.ImsApiResponse;
import com.gst_reconsilation.ims.dto.api.ImsApiResponse.*;
import com.gst_reconsilation.ims.entity.ImsFiling;
import com.gst_reconsilation.ims.entity.ImsInvoice;
import com.gst_reconsilation.ims.repository.ImsFilingRepository;
import com.gst_reconsilation.ims.repository.ImsInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImsSyncService {

    private final CompanyGSTRepository companyGSTRepository;
    private final ImsFilingRepository filingRepository;
    private final ImsInvoiceRepository invoiceRepository;
    private final ImsExcelParserService excelParserService;
    private final RestTemplate restTemplate;
    private final GstApiCredentialsProperties creds;

    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ── 1. Live API sync ─────────────────────────────────────────
    @Transactional
    public ImsSyncResponse sync(ImsSyncRequest req, Integer userId) {
        CompanyGST companyGST = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));

        ImsFiling filing = getOrCreateFiling(companyGST, req.getRetPeriod(), userId);
        filing.setSyncStatus("SYNCING");
        filing = filingRepository.save(filing);

        int rows;
        try {
            ImsApiResponse resp = callIms(companyGST.getGstNumber(), req, filing.getRetPeriod());
            invoiceRepository.deleteByFiling_Id(filing.getId()); // full-refresh on re-sync, same pattern as Gstr1/2
            List<ImsInvoice> flattened = new ArrayList<>();
            if (resp != null) {
                if (resp.getGstr1() != null) flattened.addAll(flatten("GSTR1", resp.getGstr1(), filing, userId));
                if (resp.getGstr1a() != null) flattened.addAll(flatten("GSTR1A", resp.getGstr1a(), filing, userId));
            }
            rows = invoiceRepository.saveAll(flattened).size();
            filing.setSyncStatus("SYNCED");
            filing.setSyncedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("IMS sync failed for filing {}: {}", filing.getId(), e.getMessage());
            filing.setSyncStatus("FAILED");
            rows = 0;
        }
        filingRepository.save(filing);

        return ImsSyncResponse.builder()
                .filingId(filing.getId()).retPeriod(req.getRetPeriod())
                .syncStatus(filing.getSyncStatus()).rowsSynced(rows).build();
    }

    private ImsApiResponse callIms(String gstin, ImsSyncRequest req, String retPeriod) {
        String url = UriComponentsBuilder.fromHttpUrl(creds.getImsBaseUrl() + "/supplierinvoices")
                .queryParam("gstin", gstin)
                .queryParam("email", creds.getEmail())
                .queryParam("retperiod", retPeriod)
                .queryParam("section", req.getSection())
                .queryParam("rtnTyp", req.getRtnTyp())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("gst_username", creds.getGstUsername());
        headers.set("state_cd", creds.getStateCd());
        headers.set("ip_address", creds.getIpAddress());
        headers.set("txn", UUID.randomUUID().toString().replace("-", ""));
        headers.set("client_id", creds.getClientId());
        headers.set("client_secret", creds.getClientSecret());

        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), ImsApiResponse.class).getBody();
    }

    // ── 2. Excel bulk upload ─────────────────────────────────────
    @Transactional
    public ImsUploadResponse uploadExcel(MultipartFile file, Integer companyGstId, String retPeriod, Integer userId) throws Exception {
        String originalName = file.getOriginalFilename();
        if (originalName == null || (!originalName.endsWith(".xlsx") && !originalName.endsWith(".xls"))) {
            throw new IllegalArgumentException("Only .xlsx or .xls files are accepted");
        }
        CompanyGST companyGST = companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));

        ImsFiling filing = getOrCreateFiling(companyGST, retPeriod, userId);
        invoiceRepository.deleteByFiling_Id(filing.getId());

        List<ImsInvoice> parsed;
        try (var is = file.getInputStream()) {
            parsed = excelParserService.parse(is, filing);
        }
        int rows = invoiceRepository.saveAll(parsed).size();

        filing.setSyncStatus("EXCEL");
        filing.setSyncedAt(LocalDateTime.now());
        filingRepository.save(filing);

        return ImsUploadResponse.builder()
                .filingId(filing.getId()).retPeriod(retPeriod)
                .filingStatus(filing.getSyncStatus()).rowsImported(rows).build();
    }

    // ── 3. Manual single-row entry ───────────────────────────────
    @Transactional
    public ImsInvoice createManual(ImsManualRequest req, Integer userId) {
        CompanyGST companyGST = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));

        ImsFiling filing = getOrCreateFiling(companyGST, req.getRetPeriod(), userId);

        ImsInvoice invoice = ImsInvoice.builder()
                .filing(filing).createdBy(userId)
                .section(req.getSection() != null ? req.getSection().toUpperCase() : null)
                .supplierGstin(upper(req.getSupplierGstin())).recipientGstin(upper(req.getRecipientGstin()))
                .ecommerceGstin(upper(req.getEcommerceGstin()))
                .originalInvoiceNumber(req.getOriginalInvoiceNumber()).originalInvoiceDate(parseDate(req.getOriginalInvoiceDate()))
                .invoiceNumber(req.getInvoiceNumber()).invoiceDate(parseDate(req.getInvoiceDate()))
                .invoiceValue(req.getInvoiceValue()).placeOfSupply(req.getPlaceOfSupply())
                .invoiceType(req.getInvoiceType()).reverseCharge(req.getReverseCharge()).rate(req.getRate())
                .taxableValue(nz(req.getTaxableValue())).integratedTax(nz(req.getIntegratedTax()))
                .centralTax(nz(req.getCentralTax())).stateUtTax(nz(req.getStateUtTax())).cess(nz(req.getCess()))
                .imsAction(req.getImsAction()).remarks(req.getRemarks())
                .source("MANUAL")
                .build();

        return invoiceRepository.save(invoice);
    }

    private ImsFiling getOrCreateFiling(CompanyGST companyGST, String retPeriod, Integer userId) {
        return filingRepository.findByCompanyGST_IdAndRetPeriod(companyGST.getId(), retPeriod)
                .orElseGet(() -> filingRepository.save(ImsFiling.builder()
                        .companyGST(companyGST).retPeriod(retPeriod).createdBy(userId).build()));
    }

    // ── Flattening (identical logic to the old Gstr3bImsSyncService) ─────

    private List<ImsInvoice> flatten(String gstrSource, ImsSection section, ImsFiling filing, Integer userId) {
        List<ImsInvoice> rows = new ArrayList<>();
        if (section.getB2b() != null) {
            for (var entry : section.getB2b()) {
                if (entry.getInv() == null) continue;
                for (var inv : entry.getInv()) rows.add(rowFromB2b(gstrSource, "B2B", filing, userId, entry.getCtin(), inv));
            }
        }
        if (section.getB2ba() != null) {
            for (var entry : section.getB2ba()) {
                if (entry.getInv() == null) continue;
                for (var inv : entry.getInv()) {
                    ImsInvoice row = rowFromB2b(gstrSource, "B2BA", filing, userId, entry.getCtin(), inv);
                    row.setOriginalInvoiceNumber(inv.getOriginalInvoiceNumber());
                    row.setOriginalInvoiceDate(parseDate(inv.getOriginalInvoiceDate()));
                    rows.add(row);
                }
            }
        }
        if (section.getCdnr() != null) {
            for (var entry : section.getCdnr()) {
                if (entry.getNt() == null) continue;
                for (var note : entry.getNt()) rows.add(rowFromCdnr(gstrSource, "CDNR", filing, userId, entry.getCtin(), note));
            }
        }
        if (section.getCdnra() != null) {
            for (var entry : section.getCdnra()) {
                if (entry.getNt() == null) continue;
                for (var note : entry.getNt()) {
                    ImsInvoice row = rowFromCdnr(gstrSource, "CDNRA", filing, userId, entry.getCtin(), note);
                    row.setOriginalInvoiceNumber(note.getOriginalNoteNumber());
                    row.setOriginalInvoiceDate(parseDate(note.getOriginalNoteDate()));
                    rows.add(row);
                }
            }
        }
        if (section.getEcom() != null) {
            var ecom = section.getEcom();
            if (ecom.getB2b() != null) for (var entry : ecom.getB2b()) {
                if (entry.getInv() == null) continue;
                for (var inv : entry.getInv()) rows.add(rowFromEcom(gstrSource, "ECOM_B2B", filing, userId, null, entry.getRtin(), entry.getStin(), inv));
            }
            if (ecom.getUrp2b() != null) for (var entry : ecom.getUrp2b()) {
                if (entry.getInv() == null) continue;
                for (var inv : entry.getInv()) rows.add(rowFromEcom(gstrSource, "ECOM_URP2B", filing, userId, null, entry.getRtin(), null, inv));
            }
        }
        if (section.getEcoma() != null) {
            var ecoma = section.getEcoma();
            if (ecoma.getB2ba() != null) for (var entry : ecoma.getB2ba()) {
                if (entry.getInv() == null) continue;
                for (var inv : entry.getInv()) {
                    ImsInvoice row = rowFromEcom(gstrSource, "ECOMA_B2BA", filing, userId, null, entry.getRtin(), entry.getStin(), inv);
                    row.setOriginalInvoiceNumber(inv.getOriginalInvoiceNumber());
                    row.setOriginalInvoiceDate(parseDate(inv.getOriginalInvoiceDate()));
                    rows.add(row);
                }
            }
            if (ecoma.getUrp2ba() != null) for (var entry : ecoma.getUrp2ba()) {
                if (entry.getInv() == null) continue;
                for (var inv : entry.getInv()) {
                    ImsInvoice row = rowFromEcom(gstrSource, "ECOMA_URP2BA", filing, userId, null, entry.getRtin(), null, inv);
                    row.setOriginalInvoiceNumber(inv.getOriginalInvoiceNumber());
                    row.setOriginalInvoiceDate(parseDate(inv.getOriginalInvoiceDate()));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private ImsInvoice rowFromB2b(String gstrSource, String section, ImsFiling filing, Integer userId,
                                  String supplierGstin, ImsB2bInvoice inv) {
        BigDecimal[] tax = sumItems(inv.getItms());
        return ImsInvoice.builder().filing(filing).createdBy(userId).gstrSource(gstrSource)
                .section(section).supplierGstin(upper(supplierGstin))
                .invoiceNumber(inv.getInvoiceNumber()).invoiceDate(parseDate(inv.getInvoiceDate()))
                .invoiceValue(inv.getInvoiceValue()).placeOfSupply(inv.getPos())
                .invoiceType(inv.getInvoiceType()).reverseCharge(inv.getReverseCharge()).ecommerceGstin(inv.getEtin())
                .taxableValue(tax[0]).integratedTax(tax[1]).centralTax(tax[2]).stateUtTax(tax[3]).cess(tax[4])
                .imsAction(inv.getImsAction()).remarks(inv.getRemarks()).source("SYNC").build();
    }

    private ImsInvoice rowFromCdnr(String gstrSource, String section, ImsFiling filing, Integer userId,
                                   String supplierGstin, ImsCdnrNote note) {
        BigDecimal[] tax = sumItems(note.getItms());
        return ImsInvoice.builder().filing(filing).createdBy(userId).gstrSource(gstrSource)
                .section(section).supplierGstin(upper(supplierGstin))
                .invoiceNumber(note.getNoteNumber()).invoiceDate(parseDate(note.getNoteDate()))
                .invoiceValue(note.getNoteValue()).placeOfSupply(note.getPos())
                .invoiceType(note.getInvoiceType()).reverseCharge(note.getReverseCharge())
                .taxableValue(tax[0]).integratedTax(tax[1]).centralTax(tax[2]).stateUtTax(tax[3]).cess(tax[4])
                .imsAction(note.getImsAction()).remarks(note.getRemarks()).source("SYNC").build();
    }

    private ImsInvoice rowFromEcom(String gstrSource, String section, ImsFiling filing, Integer userId,
                                   String supplierGstin, String recipientGstin, String ecommerceGstin, ImsEcomInvoice inv) {
        BigDecimal[] tax = sumItems(inv.getItms());
        return ImsInvoice.builder().filing(filing).createdBy(userId).gstrSource(gstrSource)
                .section(section).supplierGstin(upper(supplierGstin)).recipientGstin(upper(recipientGstin))
                .ecommerceGstin(upper(ecommerceGstin))
                .invoiceNumber(inv.getInvoiceNumber()).invoiceDate(parseDate(inv.getInvoiceDate()))
                .invoiceValue(inv.getInvoiceValue()).placeOfSupply(inv.getPos()).invoiceType(inv.getInvoiceType())
                .taxableValue(tax[0]).integratedTax(tax[1]).centralTax(tax[2]).stateUtTax(tax[3]).cess(tax[4])
                .imsAction(inv.getImsAction()).remarks(inv.getRemarks()).source("SYNC").build();
    }

    private java.math.BigDecimal[] sumItems(List<ImsItem> items) {
        var tv = java.math.BigDecimal.ZERO; var igst = tv; var cgst = tv; var sgst = tv; var cess = tv;
        if (items != null) {
            for (var item : items) {
                var d = item.getDetail();
                if (d == null) continue;
                tv = tv.add(nz(d.getTaxableValue())); igst = igst.add(nz(d.getIamt()));
                cgst = cgst.add(nz(d.getCamt())); sgst = sgst.add(nz(d.getSamt())); cess = cess.add(nz(d.getCsamt()));
            }
        }
        return new java.math.BigDecimal[]{tv, igst, cgst, sgst, cess};
    }

    public List<ImsInvoice> getByFiling(Integer filingId) { return invoiceRepository.findByFiling_Id(filingId); }

    private java.math.BigDecimal nz(java.math.BigDecimal v) { return v != null ? v : java.math.BigDecimal.ZERO; }
    private String upper(String s) { return s != null ? s.toUpperCase() : null; }
    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s, API_DATE); } catch (Exception e) { return null; }
    }
}