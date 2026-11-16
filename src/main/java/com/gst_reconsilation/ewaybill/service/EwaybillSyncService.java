// ewaybill/service/EwaybillSyncService.java
package com.gst_reconsilation.ewaybill.service;

import com.gst_reconsilation.apiusage.exception.ApiUsageLimitExceededException;
import com.gst_reconsilation.apiusage.service.ApiUsageService;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.config.GstApiCredentialsProperties;
import com.gst_reconsilation.ewaybill.dto.*;
import com.gst_reconsilation.ewaybill.dto.api.*;
import com.gst_reconsilation.ewaybill.entity.*;
import com.gst_reconsilation.ewaybill.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.gst_reconsilation.whitebooks.WhitebooksApiException;
import com.gst_reconsilation.whitebooks.WhitebooksClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EwaybillSyncService {

    private final CompanyGSTRepository companyGSTRepository;
    private final EwaybillFilingRepository filingRepository;
    private final EwaybillRecordRepository recordRepository;
    private final WhitebooksClient client;
    private final GstApiCredentialsProperties creds;
    private final ApiUsageService apiUsageService;

    @Transactional
    public EwaybillFiling syncByDate(EwaybillSyncByDateRequest req, Integer userId) {
        CompanyGST companyGST = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));

        EwaybillFiling filing = filingRepository
                .findByCompanyGST_IdAndSyncDate(req.getCompanyGstId(), req.getDate())
                .orElseGet(() -> EwaybillFiling.builder()
                        .companyGST(companyGST)
                        .syncDate(req.getDate())
                        .createdBy(userId)
                        .build());

        filing.setSyncStatus("SYNCING");
        filing = filingRepository.save(filing);

        try {
            apiUsageService.recordAndEnforce(req.getCompanyGstId(), ApiUsageService.EWAYBILL);
            String listUrl = UriComponentsBuilder.fromHttpUrl(creds.getEwaybillBaseUrl() + "/getewaybillsbydate")
                    .queryParam("email", creds.getEmail())
                    .queryParam("date", toEwbDate(req.getDate()))
                    .toUriString();

            EwaybillByDateEntry[] summary;
            try {
                summary = client.getData(listUrl, ewbHeaders(companyGST), EwaybillByDateEntry[].class);
            } catch (WhitebooksApiException e) {
                if (!WhitebooksApiException.isNoData(e)) throw e;
                summary = null;
            }

            int count = 0;
            if (summary != null) {
                for (var s : summary) {
                    // Skip re-fetching full detail for e-way bills already stored under this same
                    // GSTIN+date filing (a re-sync); don't skip just because the same EWB number
                    // exists under a different company/date filing.
                    if (recordRepository.existsByFiling_IdAndEwbNo(filing.getId(), s.getEwbNo())) continue;
                    fetchAndSaveDetail(s.getEwbNo(), companyGST, filing, userId);
                    count++;
                }
            }
            filing.setSyncStatus("SYNCED");
            filing.setSyncedAt(LocalDateTime.now());
            log.info("E-way bill sync for {} on {}: {} new records", companyGST.getGstNumber(), req.getDate(), count);
        } catch (ApiUsageLimitExceededException | WhitebooksApiException e) {
            filing.setSyncStatus("FAILED");
            filingRepository.save(filing);
            throw e; // limit hit / e-way bill portal error must reach the user, not a silent FAILED row
        } catch (Exception e) {
            filing.setSyncStatus("FAILED");
            log.error("E-way bill sync failed: {}", e.getMessage());
        }
        return filingRepository.save(filing);
    }

    /** Sync a single, already-known e-way bill number directly (skips the by-date list call). */
    @Transactional
    public EwaybillRecord syncSingle(Integer companyGstId, Long ewbNo, Integer userId) {
        CompanyGST companyGST = companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));
        return fetchAndSaveDetail(ewbNo, companyGST, null, userId);
    }

    private EwaybillRecord fetchAndSaveDetail(Long ewbNo, CompanyGST companyGST, EwaybillFiling filing, Integer userId) {
        apiUsageService.recordAndEnforce(companyGST.getId(), ApiUsageService.EWAYBILL);
        String url = UriComponentsBuilder.fromHttpUrl(creds.getEwaybillBaseUrl() + "/getewaybill")
                .queryParam("email", creds.getEmail())
                .queryParam("ewbNo", ewbNo)
                .toUriString();

        EwaybillDetailApiResponse d = client.getData(url, ewbHeaders(companyGST), EwaybillDetailApiResponse.class);

        if (d == null) throw new RuntimeException("No response from e-way bill API for ewbNo: " + ewbNo);

        // Scope the upsert lookup to this filing when one is known (batch sync); syncSingle() passes
        // filing=null since it isn't tied to a date-filing, so fall back to a global lookup there.
        EwaybillRecord record = (filing != null
                ? recordRepository.findByFiling_IdAndEwbNo(filing.getId(), ewbNo)
                : recordRepository.findByEwbNo(ewbNo))
                .orElseGet(() -> EwaybillRecord.builder().ewbNo(ewbNo).createdBy(userId).build());

        record.setFiling(filing);
        record.setEwbDate(d.getEwayBillDate());
        record.setGenMode(d.getGenMode());
        record.setUserGstin(d.getUserGstin());
        record.setSupplyType(d.getSupplyType());
        record.setSubSupplyType(d.getSubSupplyType());
        record.setDocType(d.getDocType());
        record.setDocNo(d.getDocNo());
        record.setDocDate(d.getDocDate());
        record.setFromGstin(d.getFromGstin());
        record.setFromTrdName(d.getFromTrdName());
        record.setFromPlace(d.getFromPlace());
        record.setFromPincode(d.getFromPincode());
        record.setFromStateCode(d.getFromStateCode());
        record.setToGstin(d.getToGstin());
        record.setToTrdName(d.getToTrdName());
        record.setToPlace(d.getToPlace());
        record.setToPincode(d.getToPincode());
        record.setToStateCode(d.getToStateCode());
        record.setTotalValue(d.getTotalValue());
        record.setTotInvValue(d.getTotInvValue());
        record.setCgstValue(d.getCgstValue());
        record.setSgstValue(d.getSgstValue());
        record.setIgstValue(d.getIgstValue());
        record.setCessValue(d.getCessValue());
        record.setTransporterId(d.getTransporterId());
        record.setTransporterName(d.getTransporterName());
        record.setStatus(d.getStatus());
        record.setValidUpto(d.getValidUpto());
        record.setExtendedTimes(d.getExtendedTimes());
        record.setRejectStatus(d.getRejectStatus());
        record.setSource("SYNC");

        return recordRepository.save(record);
    }

    // EwaybillSyncService.java — replace the existing createManual() method with this
    @Transactional
    public EwaybillRecord createManual(EwaybillManualRequest req, Integer userId) {
        if (recordRepository.existsByEwbNo(req.getEwbNo())) {
            throw new RuntimeException("An e-way bill record with this number already exists: " + req.getEwbNo());
        }
        EwaybillRecord record = EwaybillRecord.builder()
                .ewbNo(req.getEwbNo())
                .ewbDate(req.getEwbDate())
                .genMode(req.getGenMode())
                .userGstin(req.getUserGstin())
                .supplyType(req.getSupplyType())
                .subSupplyType(req.getSubSupplyType())
                .docType(req.getDocType())
                .docNo(req.getDocNo())
                .docDate(req.getDocDate())
                .fromGstin(req.getFromGstin())
                .fromTrdName(req.getFromTrdName())
                .fromPlace(req.getFromPlace())
                .fromPincode(req.getFromPincode())
                .fromStateCode(req.getFromStateCode())
                .toGstin(req.getToGstin())
                .toTrdName(req.getToTrdName())
                .toPlace(req.getToPlace())
                .toPincode(req.getToPincode())
                .toStateCode(req.getToStateCode())
                .totalValue(req.getTotalValue())
                .totInvValue(req.getTotInvValue())
                .cgstValue(req.getCgstValue())
                .sgstValue(req.getSgstValue())
                .igstValue(req.getIgstValue())
                .cessValue(req.getCessValue())
                .transporterId(req.getTransporterId())
                .transporterName(req.getTransporterName())
                .status(req.getStatus())
                .validUpto(req.getValidUpto())
                .extendedTimes(req.getExtendedTimes())
                .rejectStatus(req.getRejectStatus())
                .source("MANUAL")
                .createdBy(userId)
                .build();
        return recordRepository.save(record);
    }

    public List<EwaybillRecord> getByFiling(Integer filingId) {
        return recordRepository.findByFiling_Id(filingId);
    }

    /** Look a filing up by company + sync date, so a stored period can be reopened without re-syncing. */
    public Optional<EwaybillFiling> getFiling(Integer companyGstId, String syncDate) {
        return filingRepository.findByCompanyGST_IdAndSyncDate(companyGstId, syncDate);
    }

    // EwaybillSyncService.java — add these fields + method
    private final EwaybillExcelParserService excelParserService; // add to constructor injection

    @Transactional
    public EwaybillFiling uploadExcel(MultipartFile file, Integer companyGstId, String syncDate, Integer userId) throws Exception {
        String originalName = file.getOriginalFilename();
        if (originalName == null || (!originalName.toLowerCase().endsWith(".xlsx") && !originalName.toLowerCase().endsWith(".xls"))) {
            throw new IllegalArgumentException("Only .xlsx or .xls files are accepted");
        }
        CompanyGST companyGST = companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));

        EwaybillFiling filing = filingRepository
                .findByCompanyGST_IdAndSyncDate(companyGstId, syncDate)
                .orElseGet(() -> EwaybillFiling.builder()
                        .companyGST(companyGST).syncDate(syncDate).createdBy(userId).build());
        filing = filingRepository.save(filing);

        List<EwaybillRecord> parsed;
        try (var is = file.getInputStream()) {
            parsed = excelParserService.parse(is, filing, userId);
        }

        List<String> duplicatesWithinFile = parsed.stream()
                .collect(Collectors.groupingBy(EwaybillRecord::getEwbNo, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> String.valueOf(e.getKey()))
                .toList();
        if (!duplicatesWithinFile.isEmpty()) {
            throw new RuntimeException("This file has the same e-way bill number in more than one row: " + String.join(", ", duplicatesWithinFile));
        }

        EwaybillFiling finalFiling = filing;
        List<String> duplicateEwbNos = parsed.stream()
                .map(EwaybillRecord::getEwbNo)
                .filter(ewbNo -> recordRepository.existsByFiling_IdAndEwbNo(finalFiling.getId(), ewbNo))
                .map(String::valueOf)
                .toList();
        if (!duplicateEwbNos.isEmpty()) {
            throw new RuntimeException("These e-way bill numbers already exist for this GSTIN and sync date: " + String.join(", ", duplicateEwbNos));
        }

        recordRepository.saveAll(parsed);

        filing.setSyncStatus("EXCEL");
        filing.setSyncedAt(LocalDateTime.now());
        return filingRepository.save(filing);
    }

    /**
     * e-Way Bill uses its own Whitebooks key pair (not the GST one).
     * TODO: Whitebooks also requires a prior /authenticate call with the GSTIN's e-way bill
     * portal API username/password, and a "user_name" header on every call. Those per-GSTIN
     * credentials are not stored anywhere yet, so live e-way bill sync will fail until added.
     */
    private HttpHeaders ewbHeaders(CompanyGST companyGST) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("ip_address", creds.getIpAddress());
        headers.set("client_id", creds.getEwaybillClientId());
        headers.set("client_secret", creds.getEwaybillClientSecret());
        headers.set("gstin", companyGST.getGstNumber());
        return headers;
    }

    /** The app stores dates as yyyy-MM-dd; the e-way bill API only accepts dd/MM/yyyy (else error 601). */
    static String toEwbDate(String date) {
        if (date == null) return null;
        String d = date.trim();
        if (d.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return d.substring(8, 10) + "/" + d.substring(5, 7) + "/" + d.substring(0, 4);
        }
        return d; // assume already dd/MM/yyyy
    }
}
