// einvoice/service/EinvoiceSyncService.java
package com.gst_reconsilation.einvoice.service;

import com.gst_reconsilation.apiusage.exception.ApiUsageLimitExceededException;
import com.gst_reconsilation.apiusage.service.ApiUsageService;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.config.GstApiCredentialsProperties;
import com.gst_reconsilation.einvoice.dto.*;
import com.gst_reconsilation.einvoice.dto.api.*;
import com.gst_reconsilation.einvoice.entity.*;
import com.gst_reconsilation.einvoice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.gst_reconsilation.whitebooks.WhitebooksApiException;
import com.gst_reconsilation.whitebooks.WhitebooksClient;
import com.gst_reconsilation.whitebooks.session.GstAuthService;
import com.gst_reconsilation.whitebooks.session.GstSessionRequiredException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EinvoiceSyncService {

    private final CompanyGSTRepository companyGSTRepository;
    private final EinvoiceFilingRepository filingRepository;
    private final EinvoiceIrnRepository irnRepository;
    private final EinvoiceHsnSummaryRepository hsnRepository;
    private final WhitebooksClient client;
    private final GstAuthService gstAuthService;
    private final GstApiCredentialsProperties creds;
    private final EinvoiceExcelParserService excelParserService;
    private final ApiUsageService apiUsageService;

    @Transactional
    public EinvoiceSyncResponse sync(EinvoiceSyncRequest req, Integer userId) {
        CompanyGST companyGST = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));

        EinvoiceFiling filing = filingRepository
                .findByCompanyGST_IdAndRetPeriod(req.getCompanyGstId(), req.getRetPeriod())
                .orElseGet(() -> EinvoiceFiling.builder()
                        .companyGST(companyGST)
                        .retPeriod(req.getRetPeriod())
                        .createdBy(userId)
                        .build());

        // HTTP 428 before touching any data if there is no verified GST session
        gstAuthService.activeSession(companyGST.getId());

        filing.setSyncStatus("SYNCING");
        filing = filingRepository.save(filing);

        hsnRepository.deleteByFiling_Id(filing.getId());
        int hsnRows, irnRows;
        try {
            hsnRows = syncHsnSummary(filing, companyGST.getId(), companyGST.getGstNumber());
            irnRows = syncIrnList(filing, companyGST.getId(), companyGST.getGstNumber(), userId);
        } catch (ApiUsageLimitExceededException e) {
            filing.setSyncStatus("FAILED");
            filingRepository.save(filing);
            throw e;
        }

        filing.setSyncStatus("SYNCED");
        filing.setSyncedAt(LocalDateTime.now());
        filingRepository.save(filing);

        return EinvoiceSyncResponse.builder()
                .filingId(filing.getId())
                .retPeriod(req.getRetPeriod())
                .syncStatus("SYNCED")
                .hsnRowsSynced(hsnRows)
                .irnRowsSynced(irnRows)
                .build();
    }

    /** On-demand: fetch the full signed invoice/QR payload for one IRN and persist it onto the existing row. */
    @Transactional
    public EinvoiceIrn fetchAndStoreIrnDetail(String irn) {
        EinvoiceIrn existing = irnRepository.findFirstByIrnOrderByIdDesc(irn)
                .orElseThrow(() -> new RuntimeException("IRN not found in database: " + irn + ". Sync the period first."));

        String gstin = existing.getSupplierGstin();
        Integer companyGstId = existing.getFiling().getCompanyGST().getId();
        HttpHeaders headers = gstAuthService.headersFor(companyGstId); // HTTP 428 before anything is counted
        apiUsageService.recordAndEnforce(companyGstId, ApiUsageService.EINVOICE);
        String url = UriComponentsBuilder.fromHttpUrl(creds.getEinvoiceBaseUrl() + "/irndtl")
                .queryParam("email", creds.getEmail())
                .queryParam("gstin", gstin)
                .queryParam("irn", irn)
                .toUriString();

        EinvoiceIrnDtlApiResponse resp;
        try {
            // EinvoiceIrnDtlApiResponse already models the {"data": {...}} wrapper itself
            resp = client.rootAs(client.get(url, headers), EinvoiceIrnDtlApiResponse.class);
        } catch (WhitebooksApiException e) {
            throw gstAuthService.handleCallFailure(companyGstId, e);
        }

        // EinvoiceSyncService.fetchAndStoreIrnDetail() — add one line
        if (resp != null && resp.getData() != null) {
            var d = resp.getData();
            existing.setSignedInvoice(d.getSignedInvoice());
            existing.setSignedQrCode(d.getSignedQrCode());
            existing.setIrnStatus(d.getStatus());
            existing.setEwbValidTill(d.getEwbValidTill());
            existing.setCnlRsn(d.getCnlRsn());
            existing.setCnlRem(d.getCnlRem());
            existing.setRemarks(d.getRemarks());   // ← added
            irnRepository.save(existing);
        }
        return existing;
    }

    private int syncHsnSummary(EinvoiceFiling filing, Integer companyGstId, String gstin) {
        try {
            apiUsageService.recordAndEnforce(companyGstId, ApiUsageService.EINVOICE);
            String url = UriComponentsBuilder.fromHttpUrl(creds.getEinvoiceBaseUrl() + "/hsnsum")
                    .queryParam("email", creds.getEmail())
                    .queryParam("gstin", gstin)
                    .queryParam("ret_period", filing.getRetPeriod())
                    .toUriString();

            EinvoiceHsnApiResponse resp = fetch(url, companyGstId, EinvoiceHsnApiResponse.class);

            if (resp == null || resp.getHsn() == null) return 0;

            List<EinvoiceHsnSummary> rows = new ArrayList<>();
            rows.addAll(mapHsnEntries(resp.getHsn().getHsnB2b(), filing, "B2B"));
            rows.addAll(mapHsnEntries(resp.getHsn().getHsnB2c(), filing, "B2C"));
            return hsnRepository.saveAll(rows).size();
        } catch (ApiUsageLimitExceededException | GstSessionRequiredException e) {
            throw e; // must surface (limit hit / GST OTP needed), not be swallowed
        } catch (WhitebooksApiException e) {
            if (WhitebooksApiException.isNoData(e)) return 0;
            throw e; // surface the GST portal's message instead of a silent 0-row "SYNCED"
        } catch (Exception e) {
            log.error("E-Invoice HSN sync failed: {}", e.getMessage());
            return 0;
        }
    }

    private List<EinvoiceHsnSummary> mapHsnEntries(List<EinvoiceHsnApiResponse.HsnEntry> entries,
                                                   EinvoiceFiling filing, String type) {
        List<EinvoiceHsnSummary> rows = new ArrayList<>();
        if (entries == null) return rows;
        for (var e : entries) {
            rows.add(EinvoiceHsnSummary.builder()
                    .filing(filing).type(type)
                    .hsnSc(e.getHsnSc()).description(e.getDesc()).userDesc(e.getUserDesc())
                    .uqc(e.getUqc()).qty(e.getQty()).rate(e.getRt()).taxableValue(e.getTxval())
                    .integratedTax(e.getIamt()).centralTax(e.getCamt())
                    .stateTax(e.getSamt()).cess(e.getCsamt())
                    .build());
        }
        return rows;
    }

    private int syncIrnList(EinvoiceFiling filing, Integer companyGstId, String gstin, Integer userId) {
        try {
            apiUsageService.recordAndEnforce(companyGstId, ApiUsageService.EINVOICE);
            String url = UriComponentsBuilder.fromHttpUrl(creds.getEinvoiceBaseUrl() + "/irnlist")
                    .queryParam("email", creds.getEmail())
                    .queryParam("gstin", gstin)
                    .queryParam("suptyp", "B2B")
                    .queryParam("rtnprd", filing.getRetPeriod())
                    .toUriString();

            EinvoiceIrnListApiResponse resp = fetch(url, companyGstId, EinvoiceIrnListApiResponse.class);

            if (resp == null || resp.getIrnList() == null) return 0;

            int count = 0;
            for (var block : resp.getIrnList()) {
                if (block.getIrnDtl() == null) continue;
                for (var e : block.getIrnDtl()) {
                    // Upsert by (filing, IRN) so a re-sync of this period updates status (e.g. ACT -> CNL)
                    // instead of duplicating, without colliding with the same IRN under another filing.
                    EinvoiceIrn row = irnRepository.findByFiling_IdAndIrn(filing.getId(), e.getIrn())
                            .orElseGet(() -> EinvoiceIrn.builder().irn(e.getIrn()).createdBy(userId).build());

                    row.setFiling(filing);
                    row.setSupplierGstin(block.getCtin());
                    row.setAckNo(e.getAckNo());
                    row.setAckDt(e.getAckDt());
                    row.setDocNum(e.getDocNum());
                    row.setDocDate(e.getDocDt());
                    row.setDocType(e.getDocType());
                    row.setSupplyType(e.getSupplyType());
                    row.setTotInvAmt(e.getTotInvAmt());
                    row.setIrnStatus(e.getIrnStatus());
                    row.setEwbNo(e.getEwbNo());
                    row.setEwbDt(e.getEwbDt());
                    row.setCnlDt(e.getCnldt());
                    row.setSource("SYNC");
                    irnRepository.save(row);
                    count++;
                }
            }
            return count;
        } catch (ApiUsageLimitExceededException | GstSessionRequiredException e) {
            throw e; // must surface (limit hit / GST OTP needed), not be swallowed
        } catch (WhitebooksApiException e) {
            if (WhitebooksApiException.isNoData(e)) return 0;
            throw e; // surface the GST portal's message instead of a silent 0-row "SYNCED"
        } catch (Exception e) {
            log.error("E-Invoice IRN list sync failed: {}", e.getMessage());
            return 0;
        }
    }

    /** GST-portal e-invoice APIs use the GST keys + the GSTIN's session txn, and wrap results in "data". */
    private <T> T fetch(String url, Integer companyGstId, Class<T> type) {
        try {
            return client.getData(url, gstAuthService.headersFor(companyGstId), type);
        } catch (WhitebooksApiException e) {
            throw gstAuthService.handleCallFailure(companyGstId, e);
        }
    }

    // EinvoiceSyncService.java — replace the existing createManual() method with this
    @Transactional
    public EinvoiceIrn createManual(EinvoiceIrnManualRequest req, Integer userId) {
        CompanyGST companyGST = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));

        EinvoiceFiling filing = filingRepository
                .findByCompanyGST_IdAndRetPeriod(req.getCompanyGstId(), req.getRetPeriod())
                .orElseGet(() -> filingRepository.save(EinvoiceFiling.builder()
                        .companyGST(companyGST)
                        .retPeriod(req.getRetPeriod())
                        .syncStatus("MANUAL")
                        .createdBy(userId)
                        .build()));

        if (req.getIrn() != null && irnRepository.existsByFiling_IdAndIrn(filing.getId(), req.getIrn())) {
            throw new RuntimeException("An e-invoice record with this IRN already exists for this GSTIN and return period: " + req.getIrn());
        }

        EinvoiceIrn irn = EinvoiceIrn.builder()
                .filing(filing)
                .supplierGstin(req.getSupplierGstin())
                .irn(req.getIrn())
                .ackNo(req.getAckNo())
                .ackDt(req.getAckDt())
                .docNum(req.getDocNum())
                .docDate(req.getDocDate())
                .docType(req.getDocType())
                .supplyType(req.getSupplyType())
                .totInvAmt(req.getTotInvAmt())
                .irnStatus(req.getIrnStatus())
                .ewbNo(req.getEwbNo())
                .ewbDt(req.getEwbDt())
                .ewbValidTill(req.getEwbValidTill())
                .cnlDt(req.getCnlDt())
                .cnlRsn(req.getCnlRsn())
                .cnlRem(req.getCnlRem())
                .remarks(req.getRemarks())
                .source("MANUAL")
                .createdBy(userId)
                .build();

        return irnRepository.save(irn);
    }

    public List<EinvoiceIrn> getIrnsByFiling(Integer filingId) {
        return irnRepository.findByFiling_Id(filingId);
    }

    public Optional<EinvoiceFiling> getFiling(Integer companyGstId, String retPeriod) {
        return filingRepository.findByCompanyGST_IdAndRetPeriod(companyGstId, retPeriod);
    }

    public List<EinvoiceHsnSummary> getHsnByFiling(Integer filingId) {
        return hsnRepository.findByFiling_Id(filingId);
    }

    @Transactional
    public EinvoiceSyncResponse uploadExcel(MultipartFile file, Integer companyGstId, String retPeriod, Integer userId) throws Exception {
        String originalName = file.getOriginalFilename();
        if (originalName == null || (!originalName.toLowerCase().endsWith(".xlsx") && !originalName.toLowerCase().endsWith(".xls"))) {
            throw new IllegalArgumentException("Only .xlsx or .xls files are accepted");
        }
        CompanyGST companyGST = companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));

        EinvoiceFiling filing = filingRepository
                .findByCompanyGST_IdAndRetPeriod(companyGstId, retPeriod)
                .orElseGet(() -> EinvoiceFiling.builder()
                        .companyGST(companyGST).retPeriod(retPeriod).createdBy(userId).build());
        filing = filingRepository.save(filing);

        List<EinvoiceIrn> parsed;
        try (var is = file.getInputStream()) {
            parsed = excelParserService.parse(is, filing, userId);
        }

        List<String> duplicatesWithinFile = parsed.stream()
                .collect(Collectors.groupingBy(EinvoiceIrn::getIrn, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (!duplicatesWithinFile.isEmpty()) {
            throw new RuntimeException("This file has the same IRN in more than one row: " + String.join(", ", duplicatesWithinFile));
        }

        Integer finalFilingId = filing.getId();
        List<String> duplicateIrns = parsed.stream()
                .map(EinvoiceIrn::getIrn)
                .filter(irn -> irnRepository.existsByFiling_IdAndIrn(finalFilingId, irn))
                .toList();
        if (!duplicateIrns.isEmpty()) {
            throw new RuntimeException("These IRNs already exist for this GSTIN and return period: " + String.join(", ", duplicateIrns));
        }

        int rows = irnRepository.saveAll(parsed).size();

        filing.setSyncStatus("EXCEL");
        filing.setSyncedAt(LocalDateTime.now());
        filingRepository.save(filing);

        return EinvoiceSyncResponse.builder()
                .filingId(filing.getId()).retPeriod(retPeriod)
                .syncStatus("EXCEL").irnRowsSynced(rows).build();
    }
}