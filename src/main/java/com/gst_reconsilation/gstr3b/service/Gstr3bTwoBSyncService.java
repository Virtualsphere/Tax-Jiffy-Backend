package com.gst_reconsilation.gstr3b.service;

import com.gst_reconsilation.apiusage.service.ApiUsageService;
import com.gst_reconsilation.gstr3b.dto.TwoBCredentials;
import com.gst_reconsilation.gstr3b.dto.api.Gstr2bApiResponse;
import com.gst_reconsilation.gstr3b.dto.api.Gstr2bApiResponse.*;
import com.gst_reconsilation.gstr3b.entity.Gstr3bFiling;
import com.gst_reconsilation.gstr3b.entity.Gstr3bItcSummary;
import com.gst_reconsilation.gstr3b.repository.Gstr3bFilingRepository;
import com.gst_reconsilation.gstr3b.repository.Gstr3bItcSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.gst_reconsilation.config.GstApiCredentialsProperties;
import com.gst_reconsilation.whitebooks.GstPeriods;
import com.gst_reconsilation.whitebooks.WhitebooksApiException;
import com.gst_reconsilation.whitebooks.WhitebooksClient;
import com.gst_reconsilation.whitebooks.session.GstAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calls GET /gstr2b/all and persists the itcsumm block as flattened
 * {@link Gstr3bItcSummary} rows (bucket x category x optional sub-category),
 * used to populate table 4 (Eligible ITC) of the GSTR-3B preview.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Gstr3bTwoBSyncService {

    private final Gstr3bFilingRepository filingRepository;
    private final Gstr3bItcSummaryRepository itcSummaryRepository;
    private final WhitebooksClient client;
    private final GstAuthService gstAuthService;
    private final GstApiCredentialsProperties props;
    private final ApiUsageService apiUsageService;

    private static final String[] BUCKETS = {"itcavl", "itcunavl", "itcrev", "itcRejected"};
    private static final String[] CATEGORIES = {"nonrevsup", "isdsup", "revsup", "imports", "othersup"};

    @Transactional
    public int sync(Integer filingId, TwoBCredentials creds, Integer userId) {
        Gstr3bFiling filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Gstr3bFiling not found: " + filingId));

        Gstr2bApiResponse resp = call2b(filing, creds);

        itcSummaryRepository.deleteByFiling_Id(filingId);

        List<Gstr3bItcSummary> rows = new ArrayList<>();
        if (resp != null && resp.getData() != null && resp.getData().getItcsumm() != null) {
            ItcSumm summ = resp.getData().getItcsumm();
            addBucket(rows, filing, userId, "itcavl", summ.getItcavl());
            addBucket(rows, filing, userId, "itcunavl", summ.getItcunavl());
            addBucket(rows, filing, userId, "itcrev", summ.getItcrev());
            addBucket(rows, filing, userId, "itcRejected", summ.getItcRejected());
        }
        int saved = itcSummaryRepository.saveAll(rows).size();

        filing.setTwoBSyncStatus("SYNCED");
        filing.setTwoBSyncedAt(LocalDateTime.now());
        filingRepository.save(filing);

        log.info("GSTR-2B sync complete for Gstr3bFiling {}: {} rows", filingId, saved);
        return saved;
    }

    private Gstr2bApiResponse call2b(Gstr3bFiling filing, TwoBCredentials req) {
        if (filing.getCompanyGST() == null) throw new RuntimeException("Filing has no company GST");
        Integer companyGstId = filing.getCompanyGST().getId();
        HttpHeaders headers = gstAuthService.headersFor(companyGstId); // HTTP 428 if no verified session

        String rtnprd = req != null && req.getRtnprd() != null && !req.getRtnprd().isBlank()
                ? req.getRtnprd()
                : GstPeriods.toRetPeriod(filing.getFinancialYear(), filing.getTaxPeriod());

        UriComponentsBuilder url = UriComponentsBuilder.fromHttpUrl(props.getBaseUrl() + "/gstr2b/all")
                .queryParam("gstin", filing.getCompanyGST().getGstNumber())
                .queryParam("rtnprd", rtnprd)
                .queryParam("email", props.getEmail());
        if (req != null && req.getFilenum() != null && !req.getFilenum().isBlank()) {
            url.queryParam("filenum", req.getFilenum());
        }

        apiUsageService.recordAndEnforce(companyGstId, ApiUsageService.GSTR2B); // HTTP 429 if over the monthly limit
        try {
            // Gstr2bApiResponse already models the {"data": {...}} wrapper itself
            return client.rootAs(client.get(url.toUriString(), headers), Gstr2bApiResponse.class);
        } catch (WhitebooksApiException e) {
            throw gstAuthService.handleCallFailure(companyGstId, e);
        }
    }

    private void addBucket(List<Gstr3bItcSummary> rows, Gstr3bFiling filing, Integer userId,
                           String bucketName, CategoryGroup group) {
        if (group == null) return;
        addCategory(rows, filing, userId, bucketName, "nonrevsup", group.getNonrevsup());
        addCategory(rows, filing, userId, bucketName, "isdsup", group.getIsdsup());
        addCategory(rows, filing, userId, bucketName, "revsup", group.getRevsup());
        addCategory(rows, filing, userId, bucketName, "imports", group.getImports());
        addCategory(rows, filing, userId, bucketName, "othersup", group.getOthersup());
    }

    private void addCategory(List<Gstr3bItcSummary> rows, Gstr3bFiling filing, Integer userId,
                             String bucketName, String categoryName, TaxBlock block) {
        if (block == null) return;
        // category-level total (subCategory = null)
        rows.add(Gstr3bItcSummary.builder().filing(filing).createdBy(userId)
                .bucket(bucketName).category(categoryName).subCategory(null)
                .integratedTax(nz(block.getIgst())).centralTax(nz(block.getCgst()))
                .stateUtTax(nz(block.getSgst())).cess(nz(block.getCess())).build());

        for (Map.Entry<String, TaxBlock> sub : block.getSubCategories().entrySet()) {
            TaxBlock s = sub.getValue();
            rows.add(Gstr3bItcSummary.builder().filing(filing).createdBy(userId)
                    .bucket(bucketName).category(categoryName).subCategory(sub.getKey())
                    .integratedTax(nz(s.getIgst())).centralTax(nz(s.getCgst()))
                    .stateUtTax(nz(s.getSgst())).cess(nz(s.getCess())).build());
        }
    }

    private java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v != null ? v : java.math.BigDecimal.ZERO;
    }
}