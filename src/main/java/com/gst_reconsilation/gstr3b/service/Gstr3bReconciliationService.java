// gstr3b/service/Gstr3bReconciliationService.java
package com.gst_reconsilation.gstr3b.service;

import com.gst_reconsilation.gstr3b.entity.*;
import com.gst_reconsilation.gstr3b.repository.Gstr3bFilingRepository;
import com.gst_reconsilation.gstr3b.repository.Gstr3bReconciliationResultRepository; // create alongside the others
import com.gst_reconsilation.ims.entity.ImsInvoice;
import com.gst_reconsilation.ims.service.ImsSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class Gstr3bReconciliationService {

    private final Gstr3bFilingRepository filingRepository;
    private final Gstr2UploadService gstr2Service;
    private final ImsSyncService imsService;
    private final Gstr3bReconciliationResultRepository resultRepository;

    @Transactional
    public List<Gstr3bReconciliationResult> reconcile(Integer gstr3bFilingId) {
        Gstr3bFiling filing = filingRepository.findById(gstr3bFilingId)
                .orElseThrow(() -> new RuntimeException("Gstr3bFiling not found: " + gstr3bFilingId));

        if (filing.getGstr2FilingId() == null) throw new RuntimeException("Link a GSTR-2 filing before reconciling");
        if (filing.getImsFilingId() == null) throw new RuntimeException("Link/sync an IMS filing before reconciling");

        Map<String, Gstr2B2b> purchaseRegister = new HashMap<>();
        for (var r : gstr2Service.getB2b(filing.getGstr2FilingId())) {
            purchaseRegister.put(key(r.getGstinOfSupplier(), r.getInvoiceNumber()), r);
        }

        Map<String, ImsInvoice> imsRows = new HashMap<>();
        for (var r : imsService.getByFiling(filing.getImsFilingId())) {
            if ("B2B".equals(r.getSection())) imsRows.put(key(r.getSupplierGstin(), r.getInvoiceNumber()), r);
        }

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(purchaseRegister.keySet());
        allKeys.addAll(imsRows.keySet());

        resultRepository.deleteByFiling_Id(gstr3bFilingId);
        List<Gstr3bReconciliationResult> results = new ArrayList<>();
        for (String k : allKeys) {
            var pr = purchaseRegister.get(k);
            var ims = imsRows.get(k);
            String status;
            if (pr != null && ims != null) {
                boolean valMatch = nz(pr.getTaxableValue()).compareTo(nz(ims.getTaxableValue())) == 0
                        && nz(pr.getIntegratedTaxPaid()).compareTo(nz(ims.getIntegratedTax())) == 0;
                status = valMatch ? "MATCHED" : "VALUE_MISMATCH";
            } else if (pr != null) {
                status = "IN_PURCHASE_REGISTER_ONLY";
            } else {
                status = "IN_IMS_ONLY";
            }
            results.add(Gstr3bReconciliationResult.builder()
                    .filing(filing)
                    .supplierGstin(pr != null ? pr.getGstinOfSupplier() : ims.getSupplierGstin())
                    .invoiceNumber(pr != null ? pr.getInvoiceNumber() : ims.getInvoiceNumber())
                    .matchStatus(status)
                    .purchaseRegisterTaxableValue(pr != null ? pr.getTaxableValue() : null)
                    .imsTaxableValue(ims != null ? ims.getTaxableValue() : null)
                    .purchaseRegisterIgst(pr != null ? pr.getIntegratedTaxPaid() : null)
                    .imsIgst(ims != null ? ims.getIntegratedTax() : null)
                    .build());
        }
        return resultRepository.saveAll(results);
    }

    private String key(String gstin, String invNo) {
        return (gstin == null ? "" : gstin.toUpperCase()) + "|" + (invNo == null ? "" : invNo.trim().toUpperCase());
    }
    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}