// ims/repository/ImsInvoiceRepository.java
package com.gst_reconsilation.ims.repository;

import com.gst_reconsilation.ims.entity.ImsInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ImsInvoiceRepository extends JpaRepository<ImsInvoice, Integer> {
    List<ImsInvoice> findByFiling_Id(Integer filingId);
    List<ImsInvoice> findByFiling_IdAndSupplierGstinAndInvoiceNumber(
            Integer filingId, String supplierGstin, String invoiceNumber);
    void deleteByFiling_Id(Integer filingId);
}