package com.gst_reconsilation.admin.service;

import com.gst_reconsilation.admin.dto.AdminUserGstSummary;
import com.gst_reconsilation.admin.dto.AdminUserSummaryResponse;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import com.gst_reconsilation.user.repository.UserGSTMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** Platform-wide user view for the super-admin panel — every other user endpoint is scoped to one company. */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserDetailsRepository userDetailsRepository;
    private final UserGSTMappingRepository userGSTMappingRepository;

    public List<AdminUserSummaryResponse> listAll() {
        return userDetailsRepository.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private AdminUserSummaryResponse toSummary(UserDetails u) {
        List<AdminUserGstSummary> memberships = userGSTMappingRepository
                .findByUser_IdAndIsActiveTrue(u.getId())
                .stream()
                .map(m -> AdminUserGstSummary.builder()
                        .companyGstId(m.getCompanyGST().getId())
                        .gstNumber(m.getCompanyGST().getGstNumber())
                        .isPaymentDone(m.getCompanyGST().getIsPaymentDone())
                        .subscriptionPlanName(m.getCompanyGST().getSubscriptionPlan() != null
                                ? m.getCompanyGST().getSubscriptionPlan().getName() : null)
                        .isActive(m.getCompanyGST().getIsActive())
                        .build())
                .collect(Collectors.toList());

        var builder = AdminUserSummaryResponse.builder()
                .id(u.getId())
                .userName(u.getUserName())
                .userEmail(u.getUserEmail())
                .mobile(u.getMobile())
                .isActive(u.getIsActive())
                .isSuperAdmin(u.getIsSuperAdmin())
                .createdDate(u.getCreatedDate())
                .gstMemberships(memberships);

        if (u.getCompany() != null) {
            builder.companyId(u.getCompany().getId());
            builder.companyName(u.getCompany().getCompanyName());
        }
        return builder.build();
    }
}
