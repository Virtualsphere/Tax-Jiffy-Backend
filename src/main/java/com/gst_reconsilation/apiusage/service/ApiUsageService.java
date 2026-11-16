package com.gst_reconsilation.apiusage.service;

import com.gst_reconsilation.apiusage.entity.ApiUsageCounter;
import com.gst_reconsilation.apiusage.exception.ApiUsageLimitExceededException;
import com.gst_reconsilation.apiusage.repository.ApiUsageCounterRepository;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/**
 * Enforcement point for the 3rd-party (Whitebooks / GST portal) API limit.
 *
 * The limit is ONE monthly budget per GSTIN shared by every data API ({@link #LIMITED_TYPES});
 * the per-type counters only exist so the admin panel can show where the calls went.
 * Call {@link #recordAndEnforce} immediately before each outbound call. OTP calls are not
 * limited; they are logged separately by OtpUsageService.
 */
@Service
@RequiredArgsConstructor
public class ApiUsageService {

    public static final String GSTR1 = "GSTR1";
    public static final String GSTR2B = "GSTR2B";
    public static final String IMS = "IMS";
    public static final String EINVOICE = "EINVOICE";
    public static final String EWAYBILL = "EWAYBILL";

    /** Every API type that counts towards the GSTIN's monthly limit. */
    public static final List<String> LIMITED_TYPES = List.of(GSTR1, GSTR2B, IMS, EINVOICE, EWAYBILL);

    private final ApiUsageCounterRepository counterRepository;
    private final CompanyGSTRepository companyGSTRepository;

    /**
     * Counts one outbound call, or throws (HTTP 429, nothing counted) if the GSTIN's monthly
     * total is already used up.
     *
     * REQUIRES_NEW: the count is committed on its own, so it survives when the caller's sync
     * later fails and rolls back — the call still reached Whitebooks and must stay counted.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAndEnforce(Integer companyGstId, String apiType) {
        CompanyGST gst = findGst(companyGstId);
        int limit = effectiveLimit(gst);
        String periodKey = currentPeriod();

        // Row lock serialises concurrent calls of the same type for this GSTIN
        ApiUsageCounter counter = counterRepository
                .findForUpdate(companyGstId, apiType, periodKey)
                .orElseGet(() -> ApiUsageCounter.builder()
                        .companyGST(gst).apiType(apiType).periodKey(periodKey).callCount(0).build());

        int used = totalUsed(companyGstId, periodKey);
        if (used >= limit) {
            throw new ApiUsageLimitExceededException(
                    "Monthly GST API limit reached for " + gst.getGstNumber() + ": "
                            + used + "/" + limit + " calls used this month.");
        }

        counter.setCallCount(counter.getCallCount() + 1);
        counterRepository.save(counter);
    }

    /**
     * Fails up front when an operation that needs several calls (e.g. GSTR-1 sync = 13) can't
     * finish within the remaining budget, so it isn't cut off half-way after spending calls.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void ensureRemaining(Integer companyGstId, int callsNeeded, String operation) {
        CompanyGST gst = findGst(companyGstId);
        int limit = effectiveLimit(gst);
        int used = totalUsed(companyGstId, currentPeriod());
        if (limit - used < callsNeeded) {
            throw new ApiUsageLimitExceededException(
                    operation + " needs " + callsNeeded + " GST API calls but only " + Math.max(0, limit - used)
                            + " of " + limit + " remain this month for " + gst.getGstNumber() + ".");
        }
    }

    /** Calls used this period across all limited types. */
    public int totalUsed(Integer companyGstId, String periodKey) {
        return (int) counterRepository.sumCalls(companyGstId, periodKey, LIMITED_TYPES);
    }

    public int effectiveLimit(CompanyGST gst) {
        if (gst.getApiCallLimitOverride() != null) return gst.getApiCallLimitOverride();
        return gst.getSubscriptionPlan() != null ? gst.getSubscriptionPlan().getTransactionCount() : 0;
    }

    public static String currentPeriod() {
        return YearMonth.now().toString();
    }

    private CompanyGST findGst(Integer companyGstId) {
        return companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));
    }
}
