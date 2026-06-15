package com.gst_reconsilation.subscription.repository;


import com.gst_reconsilation.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Integer>{
    List<SubscriptionPlan> findByIsActiveTrue();
}
