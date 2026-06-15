package com.gst_reconsilation.company.service;

import com.gst_reconsilation.company.dto.CompanyGSTRequest;
import com.gst_reconsilation.company.dto.CompanyGSTResponse;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.entity.CompanyProfile;
import com.gst_reconsilation.subscription.entity.SubscriptionPlan;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.company.repository.CompanyProfileRepository;
import com.gst_reconsilation.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyGSTService {

    private final CompanyGSTRepository companyGSTRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public CompanyGSTResponse create(CompanyGSTRequest req, Integer userId) {
        if (companyGSTRepository.existsByGstNumber(req.getGstNumber())) {
            throw new RuntimeException("GST number already registered: " + req.getGstNumber());
        }
        CompanyProfile company = companyProfileRepository.findById(req.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found: " + req.getCompanyId()));

        SubscriptionPlan plan = null;
        if (req.getSubscriptionPlanId() != null) {
            plan = subscriptionPlanRepository.findById(req.getSubscriptionPlanId())
                    .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + req.getSubscriptionPlanId()));
        }

        CompanyGST gst = CompanyGST.builder()
                .company(company)
                .gstNumber(req.getGstNumber().toUpperCase())
                .subscriptionPlan(plan)
                .isPaymentDone(req.getIsPaymentDone())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .createdBy(userId)
                .build();

        return toResponse(companyGSTRepository.save(gst));
    }

    public CompanyGSTResponse getById(Integer id) {
        return companyGSTRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + id));
    }

    public List<CompanyGSTResponse> getByCompany(Integer companyId) {
        return companyGSTRepository.findByCompany_IdAndIsActiveTrue(companyId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CompanyGSTResponse update(Integer id, CompanyGSTRequest req, Integer userId) {
        CompanyGST gst = companyGSTRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + id));

        if (req.getSubscriptionPlanId() != null) {
            SubscriptionPlan plan = subscriptionPlanRepository.findById(req.getSubscriptionPlanId())
                    .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + req.getSubscriptionPlanId()));
            gst.setSubscriptionPlan(plan);
        }
        gst.setIsPaymentDone(req.getIsPaymentDone());
        gst.setStartDate(req.getStartDate());
        gst.setEndDate(req.getEndDate());
        gst.setUpdatedBy(userId);
        gst.setUpdatedDate(LocalDate.now());
        return toResponse(companyGSTRepository.save(gst));
    }

    public void deactivate(Integer id, Integer userId) {
        CompanyGST gst = companyGSTRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + id));
        gst.setIsActive(false);
        gst.setUpdatedBy(userId);
        gst.setUpdatedDate(LocalDate.now());
        companyGSTRepository.save(gst);
    }

    private CompanyGSTResponse toResponse(CompanyGST g) {
        CompanyGSTResponse r = new CompanyGSTResponse();
        r.setId(g.getId());
        r.setGstNumber(g.getGstNumber());
        r.setIsPaymentDone(g.getIsPaymentDone());
        r.setStartDate(g.getStartDate());
        r.setEndDate(g.getEndDate());
        r.setIsActive(g.getIsActive());
        if (g.getCompany() != null) {
            r.setCompanyId(g.getCompany().getId());
            r.setCompanyName(g.getCompany().getCompanyName());
        }
        if (g.getSubscriptionPlan() != null) {
            r.setSubscriptionPlanName(g.getSubscriptionPlan().getName());
        }
        return r;
    }
}