// einvoice/repository/EinvoiceHsnSummaryRepository.java
package com.gst_reconsilation.einvoice.repository;

import com.gst_reconsilation.einvoice.entity.EinvoiceHsnSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EinvoiceHsnSummaryRepository extends JpaRepository<EinvoiceHsnSummary, Integer> {
    List<EinvoiceHsnSummary> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}