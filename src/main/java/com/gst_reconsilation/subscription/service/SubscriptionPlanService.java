package com.gst_reconsilation.subscription.service;

import com.gst_reconsilation.subscription.dto.SubscriptionPlanRequest;
import com.gst_reconsilation.subscription.dto.SubscriptionPlanResponse;
import com.gst_reconsilation.subscription.entity.SubscriptionPlan;
import com.gst_reconsilation.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository repository;

    public SubscriptionPlanResponse create(SubscriptionPlanRequest req, Integer userId) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(req.getName())
                .userCount(req.getUserCount())
                .transactionCount(req.getTransactionCount())
                .planAmount(req.getPlanAmount())
                .createdBy(userId)
                .build();
        return toResponse(repository.save(plan));
    }

    public List<SubscriptionPlanResponse> getAll() {
        return repository.findByIsActiveTrue().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public SubscriptionPlanResponse getById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + id));
    }

    public SubscriptionPlanResponse update(Integer id, SubscriptionPlanRequest req, Integer userId) {
        SubscriptionPlan plan = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + id));
        plan.setName(req.getName());
        plan.setUserCount(req.getUserCount());
        plan.setTransactionCount(req.getTransactionCount());
        plan.setPlanAmount(req.getPlanAmount());
        plan.setUpdatedBy(userId);
        plan.setUpdatedDate(LocalDate.now());
        return toResponse(repository.save(plan));
    }

    public void deactivate(Integer id, Integer userId) {
        SubscriptionPlan plan = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + id));
        plan.setIsActive(false);
        plan.setUpdatedBy(userId);
        plan.setUpdatedDate(LocalDate.now());
        repository.save(plan);
    }

    private SubscriptionPlanResponse toResponse(SubscriptionPlan p) {
        SubscriptionPlanResponse r = new SubscriptionPlanResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setUserCount(p.getUserCount());
        r.setTransactionCount(p.getTransactionCount());
        r.setPlanAmount(p.getPlanAmount());
        r.setIsActive(p.getIsActive());
        return r;
    }
}