package com.gst_reconsilation.apiusage.repository;

import com.gst_reconsilation.apiusage.entity.ApiUsageCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiUsageCounterRepository extends JpaRepository<ApiUsageCounter, Integer> {

    /**
     * Locked read for the increment-and-check in ApiUsageService — call volume per GST is low
     * (a handful of syncs/minute at most), so a DB row lock is simpler and sufficient here
     * versus introducing Redis/atomic counters for true high-throughput rate limiting.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ApiUsageCounter c where c.companyGST.id = :companyGstId and c.apiType = :apiType and c.periodKey = :periodKey")
    Optional<ApiUsageCounter> findForUpdate(
            @Param("companyGstId") Integer companyGstId,
            @Param("apiType") String apiType,
            @Param("periodKey") String periodKey);

    List<ApiUsageCounter> findByCompanyGST_Id(Integer companyGstId);
    List<ApiUsageCounter> findByPeriodKey(String periodKey);

    /** Plain (non-locking) read for admin listing — findForUpdate is reserved for the enforcement path. */
    Optional<ApiUsageCounter> findByCompanyGST_IdAndApiTypeAndPeriodKey(Integer companyGstId, String apiType, String periodKey);
}
