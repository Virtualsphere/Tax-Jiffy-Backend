package com.gst_reconsilation.subscription.repository;

import com.gst_reconsilation.subscription.entity.SubscriptionPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubscriptionPurchaseRepository extends JpaRepository<SubscriptionPurchase, Integer> {
    List<SubscriptionPurchase> findAllByOrderByCreatedDateDesc();
    List<SubscriptionPurchase> findByCompanyGST_IdOrderByCreatedDateDesc(Integer companyGstId);
}
