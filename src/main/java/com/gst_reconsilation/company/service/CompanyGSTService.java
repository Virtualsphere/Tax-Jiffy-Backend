package com.gst_reconsilation.company.service;

import com.gst_reconsilation.company.dto.CompanyGSTRequest;
import com.gst_reconsilation.company.dto.CompanyGSTResponse;
import com.gst_reconsilation.company.dto.PurchaseSubscriptionRequest;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.entity.CompanyProfile;
import com.gst_reconsilation.roles.entity.Roles;
import com.gst_reconsilation.roles.repository.RolesRepository;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import com.gst_reconsilation.user.repository.UserGSTMappingRepository;
import com.gst_reconsilation.subscription.entity.SubscriptionPlan;
import com.gst_reconsilation.subscription.entity.SubscriptionPurchase;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.company.repository.CompanyProfileRepository;
import com.gst_reconsilation.subscription.repository.SubscriptionPlanRepository;
import com.gst_reconsilation.subscription.repository.SubscriptionPurchaseRepository;
import com.gst_reconsilation.user.entity.UserGSTMapping;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyGSTService {

    private final CompanyGSTRepository companyGSTRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final RolesRepository rolesRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final UserGSTMappingRepository userGSTMappingRepository;
    private final SubscriptionPurchaseRepository subscriptionPurchaseRepository;


    public CompanyGSTResponse create(CompanyGSTRequest req, Integer userId) {
        if (companyGSTRepository.existsByGstNumber(req.getGstNumber())) {
            throw new RuntimeException("GST number already registered: " + req.getGstNumber());
        }
        CompanyProfile company = companyProfileRepository.findById(req.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found: " + req.getCompanyId()));

        if (!company.getOwnerUserId().equals(userId)) {
            throw new RuntimeException("You can only add GST to your own company");
        }

        CompanyGST gst = CompanyGST.builder()
                .company(company)
                .gstNumber(req.getGstNumber().toUpperCase())
                .isPaymentDone(false)
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

    /** Active + deactivated GST numbers, for a full subscription/billing history view. */
    public List<CompanyGSTResponse> getAllByCompany(Integer companyId) {
        return companyGSTRepository.findByCompany_Id(companyId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CompanyGSTResponse update(Integer id, CompanyGSTRequest req, Integer userId) {
        CompanyGST gst = companyGSTRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + id));

        if (!gst.getCompany().getOwnerUserId().equals(userId)) {
            throw new RuntimeException("You can only update your own company's GST");
        }

        if (req.getGstNumber() != null && !req.getGstNumber().equalsIgnoreCase(gst.getGstNumber())) {
            if (companyGSTRepository.existsByGstNumber(req.getGstNumber())) {
                throw new RuntimeException("GST number already registered: " + req.getGstNumber());
            }
            gst.setGstNumber(req.getGstNumber().toUpperCase());
        }

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
            r.setSubscriptionPlanId(g.getSubscriptionPlan().getId());
            r.setSubscriptionPlanName(g.getSubscriptionPlan().getName());
            r.setPlanAmount(g.getSubscriptionPlan().getPlanAmount());
            r.setPlanUserCount(g.getSubscriptionPlan().getUserCount());
            r.setPlanTransactionCount(g.getSubscriptionPlan().getTransactionCount());
        }
        return r;
    }

    /**
     * First purchase for a GST. Auto-creates (or reuses) an ADMIN role scoped
     * to this company + GST — no more dependency on a globally-seeded "ADMIN"
     * role, which is what was crashing this endpoint before.
     */
    @Transactional
    public CompanyGSTResponse purchaseSubscription(Integer companyGstId, PurchaseSubscriptionRequest req, Integer userId) {
        CompanyGST gst = companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));

        if (Boolean.TRUE.equals(gst.getIsPaymentDone())) {
            throw new RuntimeException("Subscription already active for this GST number. Use the upgrade endpoint instead.");
        }

        if (!gst.getCompany().getOwnerUserId().equals(userId)) {
            throw new RuntimeException("You can only purchase subscription for your own GST");
        }

        SubscriptionPlan plan = subscriptionPlanRepository.findById(req.getSubscriptionPlanId())
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        LocalDateTime start = LocalDateTime.now();
        gst.setSubscriptionPlan(plan);
        gst.setIsPaymentDone(true);
        gst.setStartDate(start);
        gst.setEndDate(start.plusMonths(1));
        gst.setUpdatedBy(userId);
        gst.setUpdatedDate(LocalDate.now());
        companyGSTRepository.save(gst);
        recordPurchaseHistory(gst, plan, "PURCHASE", userId);

        Roles adminRole = getOrCreateRoleForGST(gst, "ADMIN", "Admin role for GST " + gst.getGstNumber(), userId);

        UserDetails user = userDetailsRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserGSTMapping adminMapping = UserGSTMapping.builder()
                .user(user)
                .companyGST(gst)
                .role(adminRole)
                .isAdmin(true)
                .createdBy(userId)
                .build();
        userGSTMappingRepository.save(adminMapping);

        return toResponse(gst);
    }

    /**
     * NEW: upgrade an already-active subscription plan for a GST.
     * Callable by the company owner or any user already marked as admin
     * for this GST (not just the original buyer).
     */
    @Transactional
    public CompanyGSTResponse upgradeSubscription(Integer companyGstId, PurchaseSubscriptionRequest req, Integer userId) {
        CompanyGST gst = companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));

        if (!Boolean.TRUE.equals(gst.getIsPaymentDone())) {
            throw new RuntimeException("No active subscription to upgrade. Use the purchase endpoint instead.");
        }

        assertCallerIsAdminOfGST(userId, gst);

        SubscriptionPlan plan = subscriptionPlanRepository.findById(req.getSubscriptionPlanId())
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        LocalDateTime start = LocalDateTime.now();
        gst.setSubscriptionPlan(plan);
        gst.setStartDate(start);
        gst.setEndDate(start.plusMonths(1));
        gst.setUpdatedBy(userId);
        gst.setUpdatedDate(LocalDate.now());

        CompanyGST saved = companyGSTRepository.save(gst);
        recordPurchaseHistory(saved, plan, "UPGRADE", userId);
        return toResponse(saved);
    }

    /** Appends one history row per purchase/upgrade — CompanyGST itself stays a single mutable snapshot. */
    private void recordPurchaseHistory(CompanyGST gst, SubscriptionPlan plan, String transactionType, Integer userId) {
        subscriptionPurchaseRepository.save(SubscriptionPurchase.builder()
                .companyGST(gst)
                .gstNumber(gst.getGstNumber())
                .company(gst.getCompany())
                .subscriptionPlan(plan)
                .planNameSnapshot(plan.getName())
                .planAmountSnapshot(plan.getPlanAmount())
                .transactionType(transactionType)
                .startDate(gst.getStartDate())
                .endDate(gst.getEndDate())
                .isPaymentDone(true)
                .createdBy(userId)
                .build());
    }

    private Roles getOrCreateRoleForGST(CompanyGST gst, String roleName, String description, Integer userId) {
        return rolesRepository.findByRoleNameAndCompanyGST_IdAndIsActiveTrue(roleName, gst.getId())
                .orElseGet(() -> rolesRepository.save(Roles.builder()
                        .roleName(roleName)
                        .description(description)
                        .company(gst.getCompany())
                        .companyGST(gst)
                        .createdBy(userId)
                        .build()));
    }

    private void assertCallerIsAdminOfGST(Integer callerId, CompanyGST gst) {
        UserDetails caller = userDetailsRepository.findById(callerId)
                .orElseThrow(() -> new RuntimeException("Caller not found"));
        if (Boolean.TRUE.equals(caller.getIsSuperAdmin())) return;
        if (gst.getCompany().getOwnerUserId().equals(callerId)) return;

        boolean isAdmin = userGSTMappingRepository
                .findByUser_IdAndIsActiveTrueAndIsAdminTrue(callerId)
                .stream()
                .anyMatch(m -> m.getCompanyGST().getId().equals(gst.getId()));

        if (!isAdmin) throw new RuntimeException("Only the GST admin can upgrade this subscription");
    }
}