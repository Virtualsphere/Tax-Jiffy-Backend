package com.gst_reconsilation.admin.service;

import com.gst_reconsilation.admin.dto.AdminGstSummaryResponse;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.rolesmapping.repository.RoleMappingRepository;
import com.gst_reconsilation.user.repository.UserGSTMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** Platform-wide GST view for the super-admin panel — CompanyGSTService's endpoints are all scoped to one company. */
@Service
@RequiredArgsConstructor
public class AdminGstService {

    private final CompanyGSTRepository companyGSTRepository;
    private final UserGSTMappingRepository userGSTMappingRepository;
    private final RoleMappingRepository roleMappingRepository;

    public List<AdminGstSummaryResponse> listAll() {
        return companyGSTRepository.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public AdminGstSummaryResponse getById(Integer id) {
        CompanyGST gst = companyGSTRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + id));
        return toSummary(gst);
    }

    private AdminGstSummaryResponse toSummary(CompanyGST g) {
        var builder = AdminGstSummaryResponse.builder()
                .id(g.getId())
                .gstNumber(g.getGstNumber())
                .isPaymentDone(g.getIsPaymentDone())
                .startDate(g.getStartDate())
                .endDate(g.getEndDate())
                .isActive(g.getIsActive())
                .activeUserCount(userGSTMappingRepository.countByCompanyGST_IdAndIsActiveTrue(g.getId()))
                .pageAccessCount(roleMappingRepository.countByCompanyGST_Id(g.getId()));

        if (g.getCompany() != null) {
            builder.companyId(g.getCompany().getId());
            builder.companyName(g.getCompany().getCompanyName());
        }
        if (g.getSubscriptionPlan() != null) {
            builder.subscriptionPlanId(g.getSubscriptionPlan().getId());
            builder.subscriptionPlanName(g.getSubscriptionPlan().getName());
            builder.planAmount(g.getSubscriptionPlan().getPlanAmount());
            builder.planUserCount(g.getSubscriptionPlan().getUserCount());
            builder.planTransactionCount(g.getSubscriptionPlan().getTransactionCount());
        }
        return builder.build();
    }
}
