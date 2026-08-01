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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

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
                    // Skip re-fetching full detail for e-way bills already stored, unless status changed.
                    if (recordRepository.existsByEwbNo(s.getEwbNo())) continue;
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

        EwaybillRecord record = recordRepository.findByEwbNo(ewbNo)
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

    @Transactional
    public EwaybillRecord createManual(EwaybillManualRequest req, Integer userId) {
        if (recordRepository.existsByEwbNo(req.getEwbNo())) {
            throw new RuntimeException("An e-way bill record with this number already exists: " + req.getEwbNo());
        }
        EwaybillRecord record = EwaybillRecord.builder()
                .ewbNo(req.getEwbNo())
                .ewbDate(req.getEwbDate())
                .docType(req.getDocType())
                .docNo(req.getDocNo())
                .docDate(req.getDocDate())
                .fromGstin(req.getFromGstin())
                .fromTrdName(req.getFromTrdName())
                .fromPlace(req.getFromPlace())
                .toGstin(req.getToGstin())
                .toTrdName(req.getToTrdName())
                .toPlace(req.getToPlace())
                .totalValue(req.getTotalValue())
                .totInvValue(req.getTotInvValue())
                .cgstValue(req.getCgstValue())
                .sgstValue(req.getSgstValue())
                .igstValue(req.getIgstValue())
                .cessValue(req.getCessValue())
                .transporterName(req.getTransporterName())
                .status(req.getStatus())
                .validUpto(req.getValidUpto())
                .source("MANUAL")
                .createdBy(userId)
                .build();
        return recordRepository.save(record);
    }

    public List<EwaybillRecord> getByFiling(Integer filingId) {
        return recordRepository.findByFiling_Id(filingId);
    }
}