package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr3bImsInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr3bImsInvoiceRepository extends JpaRepository<Gstr3bImsInvoice, Integer> {
    List<Gstr3bImsInvoice> findByFiling_Id(Integer filingId);
    List<Gstr3bImsInvoice> findByFiling_IdAndSupplierGstinAndInvoiceNumber(
            Integer filingId, String supplierGstin, String invoiceNumber);
    void deleteByFiling_Id(Integer filingId);
}