package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1EwaybillReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1EwaybillReconciliationResultRepository extends JpaRepository<Gstr1EwaybillReconciliationResult, Integer> {
    List<Gstr1EwaybillReconciliationResult> findByFiling_Id(Integer filingId);
    List<Gstr1EwaybillReconciliationResult> findByFiling_IdAndMatchStatus(Integer filingId, String matchStatus);
    void deleteByFiling_Id(Integer filingId);
}
