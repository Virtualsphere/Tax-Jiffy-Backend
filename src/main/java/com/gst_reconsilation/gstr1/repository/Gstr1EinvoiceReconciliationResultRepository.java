// gstr1/repository/Gstr1EinvoiceReconciliationResultRepository.java
package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1EinvoiceReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1EinvoiceReconciliationResultRepository extends JpaRepository<Gstr1EinvoiceReconciliationResult, Integer> {
    List<Gstr1EinvoiceReconciliationResult> findByFiling_Id(Integer filingId);
    List<Gstr1EinvoiceReconciliationResult> findByFiling_IdAndMatchStatus(Integer filingId, String matchStatus);
    void deleteByFiling_Id(Integer filingId);
}