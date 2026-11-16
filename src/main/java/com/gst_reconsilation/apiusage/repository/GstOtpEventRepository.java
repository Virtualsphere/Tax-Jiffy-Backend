package com.gst_reconsilation.apiusage.repository;

import com.gst_reconsilation.apiusage.entity.GstOtpEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GstOtpEventRepository extends JpaRepository<GstOtpEvent, Integer> {

    /**
     * Per-GSTIN totals in [from, to): companyGstId, requests, failed requests, verifies,
     * failed verifies, last event time.
     */
    @Query("select e.companyGST.id, "
            + "sum(case when e.action = 'REQUEST' then 1 else 0 end), "
            + "sum(case when e.action = 'REQUEST' and e.success = false then 1 else 0 end), "
            + "sum(case when e.action = 'VERIFY' then 1 else 0 end), "
            + "sum(case when e.action = 'VERIFY' and e.success = false then 1 else 0 end), "
            + "max(e.createdAt) "
            + "from GstOtpEvent e where e.createdAt >= :from and e.createdAt < :to "
            + "group by e.companyGST.id")
    List<Object[]> summarize(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<GstOtpEvent> findByCompanyGST_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Integer companyGstId, LocalDateTime from, LocalDateTime to);
}
