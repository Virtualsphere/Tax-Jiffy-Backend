package com.gst_reconsilation.admin.service;

import com.gst_reconsilation.admin.dto.ApiUsageSummaryResponse;
import com.gst_reconsilation.apiusage.entity.ApiUsageCounter;
import com.gst_reconsilation.apiusage.repository.ApiUsageCounterRepository;
import com.gst_reconsilation.apiusage.service.ApiUsageService;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminApiUsageService {

    private static final List<String> API_TYPES = List.of("EINVOICE", "EWAYBILL", "IMS");

    private final CompanyGSTRepository companyGSTRepository;
    private final ApiUsageCounterRepository counterRepository;
    private final ApiUsageService apiUsageService;

    /** Current-period usage for every GST x API type on the platform. */
    public List<ApiUsageSummaryResponse> listAll() {
        String periodKey = YearMonth.now().toString();
        return companyGSTRepository.findAll().stream()
                .flatMap(gst -> API_TYPES.stream().map(apiType -> toSummary(gst, apiType, periodKey)))
                .collect(Collectors.toList());
    }

    public List<ApiUsageSummaryResponse> listByGst(Integer companyGstId) {
        CompanyGST gst = companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));
        String periodKey = YearMonth.now().toString();
        return API_TYPES.stream().map(apiType -> toSummary(gst, apiType, periodKey)).collect(Collectors.toList());
    }

    @Transactional
    public void setOverride(Integer companyGstId, Integer override) {
        CompanyGST gst = companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));
        gst.setApiCallLimitOverride(override);
        companyGSTRepository.save(gst);
    }

    private ApiUsageSummaryResponse toSummary(CompanyGST gst, String apiType, String periodKey) {
        int callCount = counterRepository.findByCompanyGST_IdAndApiTypeAndPeriodKey(gst.getId(), apiType, periodKey)
                .map(ApiUsageCounter::getCallCount)
                .orElse(0);
        Integer planDefault = gst.getSubscriptionPlan() != null ? gst.getSubscriptionPlan().getTransactionCount() : null;

        return ApiUsageSummaryResponse.builder()
                .companyGstId(gst.getId())
                .gstNumber(gst.getGstNumber())
                .companyName(gst.getCompany() != null ? gst.getCompany().getCompanyName() : null)
                .apiType(apiType)
                .periodKey(periodKey)
                .callCount(callCount)
                .effectiveLimit(apiUsageService.effectiveLimit(gst))
                .planDefaultLimit(planDefault)
                .override(gst.getApiCallLimitOverride())
                .build();
    }
}
