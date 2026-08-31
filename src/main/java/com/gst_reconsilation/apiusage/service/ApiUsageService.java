package com.gst_reconsilation.apiusage.service;

import com.gst_reconsilation.apiusage.entity.ApiUsageCounter;
import com.gst_reconsilation.apiusage.exception.ApiUsageLimitExceededException;
import com.gst_reconsilation.apiusage.repository.ApiUsageCounterRepository;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/**
 * Enforcement point for the 3rd-party GST-portal API limit. Call recordAndEnforce() BEFORE each
 * outbound call in EinvoiceSyncService/ImsSyncService/EwaybillSyncService — it throws (hard block,
 * nothing is incremented) once the GST's effective limit for the current period is already used up,
 * else it increments and lets the caller proceed.
 */
@Service
@RequiredArgsConstructor
public class ApiUsageService {

    private final ApiUsageCounterRepository counterRepository;
    private final CompanyGSTRepository companyGSTRepository;

    @Transactional
    public void recordAndEnforce(Integer companyGstId, String apiType) {
        CompanyGST gst = companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));

        int effectiveLimit = effectiveLimit(gst);
        String periodKey = YearMonth.now().toString();

        ApiUsageCounter counter = counterRepository
                .findForUpdate(companyGstId, apiType, periodKey)
                .orElseGet(() -> ApiUsageCounter.builder()
                        .companyGST(gst).apiType(apiType).periodKey(periodKey).callCount(0).build());

        if (counter.getCallCount() >= effectiveLimit) {
            throw new ApiUsageLimitExceededException(
                    "API call limit reached for GST " + gst.getGstNumber() + " (" + apiType + "): "
                            + counter.getCallCount() + "/" + effectiveLimit + " calls used this period.");
        }

        counter.setCallCount(counter.getCallCount() + 1);
        counterRepository.save(counter);
    }

    public int effectiveLimit(CompanyGST gst) {
        if (gst.getApiCallLimitOverride() != null) return gst.getApiCallLimitOverride();
        return gst.getSubscriptionPlan() != null ? gst.getSubscriptionPlan().getTransactionCount() : 0;
    }
}
