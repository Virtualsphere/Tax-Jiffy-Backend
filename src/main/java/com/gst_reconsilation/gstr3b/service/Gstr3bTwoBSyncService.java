package com.gst_reconsilation.gstr3b.service;

import com.gst_reconsilation.gstr3b.dto.TwoBCredentials;
import com.gst_reconsilation.gstr3b.dto.api.Gstr2bApiResponse;
import com.gst_reconsilation.gstr3b.dto.api.Gstr2bApiResponse.*;
import com.gst_reconsilation.gstr3b.entity.Gstr3bFiling;
import com.gst_reconsilation.gstr3b.entity.Gstr3bItcSummary;
import com.gst_reconsilation.gstr3b.repository.Gstr3bFilingRepository;
import com.gst_reconsilation.gstr3b.repository.Gstr3bItcSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
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
    private final RestTemplate restTemplate;

    @Value("${gstr3b.api.gstr2b-base-url:https://apisandbox.whitebooks.in}")
    private String baseUrl;

    private static final String[] BUCKETS = {"itcavl", "itcunavl", "itcrev", "itcRejected"};
    private static final String[] CATEGORIES = {"nonrevsup", "isdsup", "revsup", "imports", "othersup"};

    @Transactional
    public int sync(Integer filingId, TwoBCredentials creds, Integer userId) {
        Gstr3bFiling filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Gstr3bFiling not found: " + filingId));

        Gstr2bApiResponse resp = call2b(creds);

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

    private Gstr2bApiResponse call2b(TwoBCredentials creds) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/gstr2b/all")
                .queryParam("gstin", creds.getGstin())
                .queryParam("rtnprd", creds.getRtnprd())
                .queryParam("filenum", creds.getFilenum())
                .queryParam("email", creds.getEmail())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("get_username", creds.getGstUsername());
        headers.set("state_cd", creds.getStateCd());
        headers.set("ip_address", creds.getIpAddress());
        headers.set("txn", creds.getTxn());
        headers.set("client_id", creds.getClientId());
        headers.set("client_secret", creds.getClientSecret());

        ResponseEntity<Gstr2bApiResponse> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), Gstr2bApiResponse.class);
        return resp.getBody();
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