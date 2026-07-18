package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr3bItcSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr3bItcSummaryRepository extends JpaRepository<Gstr3bItcSummary, Integer> {
    List<Gstr3bItcSummary> findByFiling_Id(Integer filingId);
    List<Gstr3bItcSummary> findByFiling_IdAndBucket(Integer filingId, String bucket);
    void deleteByFiling_Id(Integer filingId);
}