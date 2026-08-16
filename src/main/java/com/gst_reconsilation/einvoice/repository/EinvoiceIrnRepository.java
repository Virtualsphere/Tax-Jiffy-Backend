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

    /** Global lookup used only where no filing context is available (e.g. on-demand detail fetch by IRN alone). */
    Optional<EinvoiceIrn> findFirstByIrnOrderByIdDesc(String irn);

    Optional<EinvoiceIrn> findByFiling_IdAndIrn(Integer filingId, String irn);
    boolean existsByFiling_IdAndIrn(Integer filingId, String irn);
}