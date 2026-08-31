package com.gst_reconsilation.admin.service;

import com.gst_reconsilation.subscription.dto.SubscriptionPurchaseResponse;
import com.gst_reconsilation.subscription.entity.SubscriptionPurchase;
import com.gst_reconsilation.subscription.repository.SubscriptionPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** Read-only admin view over subscription purchase/upgrade history. */
@Service
@RequiredArgsConstructor
public class AdminSubscriptionService {

    private final SubscriptionPurchaseRepository subscriptionPurchaseRepository;

    public List<SubscriptionPurchaseResponse> listAll() {
        return subscriptionPurchaseRepository.findAllByOrderByCreatedDateDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<SubscriptionPurchaseResponse> listByGst(Integer companyGstId) {
        return subscriptionPurchaseRepository.findByCompanyGST_IdOrderByCreatedDateDesc(companyGstId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private SubscriptionPurchaseResponse toResponse(SubscriptionPurchase p) {
        var builder = SubscriptionPurchaseResponse.builder()
                .id(p.getId())
                .gstNumber(p.getGstNumber())
                .planNameSnapshot(p.getPlanNameSnapshot())
                .planAmountSnapshot(p.getPlanAmountSnapshot())
                .transactionType(p.getTransactionType())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .isPaymentDone(p.getIsPaymentDone())
                .createdDate(p.getCreatedDate())
                .createdBy(p.getCreatedBy());

        if (p.getCompany() != null) {
            builder.companyId(p.getCompany().getId());
            builder.companyName(p.getCompany().getCompanyName());
        }
        if (p.getSubscriptionPlan() != null) {
            builder.subscriptionPlanId(p.getSubscriptionPlan().getId());
        }
        return builder.build();
    }
}
