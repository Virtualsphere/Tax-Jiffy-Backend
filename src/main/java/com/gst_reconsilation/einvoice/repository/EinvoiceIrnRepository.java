// einvoice/repository/EinvoiceIrnRepository.java
package com.gst_reconsilation.einvoice.repository;

import com.gst_reconsilation.einvoice.entity.EinvoiceIrn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EinvoiceIrnRepository extends JpaRepository<EinvoiceIrn, Integer> {
    List<EinvoiceIrn> findByFiling_Id(Integer filingId);
    Optional<EinvoiceIrn> findByIrn(String irn);
    boolean existsByIrn(String irn);
}