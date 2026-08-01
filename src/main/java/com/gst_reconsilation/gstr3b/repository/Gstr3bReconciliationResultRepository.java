// gstr3b/repository/Gstr3bReconciliationResultRepository.java
package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr3bReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr3bReconciliationResultRepository extends JpaRepository<Gstr3bReconciliationResult, Integer> {
    List<Gstr3bReconciliationResult> findByFiling_Id(Integer filingId);
    List<Gstr3bReconciliationResult> findByFiling_IdAndMatchStatus(Integer filingId, String matchStatus);
    void deleteByFiling_Id(Integer filingId);
}