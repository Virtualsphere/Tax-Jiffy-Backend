// ewaybill/service/EwaybillSyncService.java
package com.gst_reconsilation.ewaybill.service;

import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.config.GstApiCredentialsProperties;
import com.gst_reconsilation.ewaybill.dto.*;
import com.gst_reconsilation.ewaybill.dto.api.*;
import com.gst_reconsilation.ewaybill.entity.*;
import com.gst_reconsilation.ewaybill.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
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
    private final RestTemplate restTemplate;
    private final GstApiCredentialsProperties creds;

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
            String listUrl = UriComponentsBuilder.fromHttpUrl(creds.getEwaybillBaseUrl() + "/getewaybillsbydate")
                    .queryParam("email", creds.getEmail())
                    .queryParam("date", req.getDate())
                    .toUriString();

            HttpHeaders listHeaders = new HttpHeaders();
            listHeaders.set("ip_address", creds.getIpAddress());
            listHeaders.set("client_id", creds.getClientId());
            listHeaders.set("client_secret", creds.getClientSecret());
            listHeaders.set("gstin", companyGST.getGstNumber());

            EwaybillByDateEntry[] summary = restTemplate.exchange(
                    listUrl, HttpMethod.GET, new HttpEntity<>(listHeaders), EwaybillByDateEntry[].class).getBody();

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
        String url = UriComponentsBuilder.fromHttpUrl(creds.getEwaybillBaseUrl() + "/getewaybill")
                .queryParam("email", creds.getEmail())
                .queryParam("ewbNo", ewbNo)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("ip_address", creds.getIpAddress());
        headers.set("client_id", creds.getClientId());
        headers.set("client_secret", creds.getClientSecret());
        headers.set("gstin", companyGST.getGstNumber());

        EwaybillDetailApiResponse d = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), EwaybillDetailApiResponse.class).getBody();

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
        if (originalName == null || (!originalName.endsWith(".xlsx") && !originalName.endsWith(".xls"))) {
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
}