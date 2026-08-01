// einvoice/repository/EinvoiceFilingRepository.java
package com.gst_reconsilation.einvoice.repository;

import com.gst_reconsilation.einvoice.entity.EinvoiceFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EinvoiceFilingRepository extends JpaRepository<EinvoiceFiling, Integer> {
    Optional<EinvoiceFiling> findByCompanyGST_IdAndRetPeriod(Integer companyGstId, String retPeriod);
}