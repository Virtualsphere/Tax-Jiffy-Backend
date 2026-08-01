// gstr1/service/Gstr1EinvoiceReconciliationService.java
package com.gst_reconsilation.gstr1.service;

import com.gst_reconsilation.einvoice.dto.EinvoiceSyncRequest;
import com.gst_reconsilation.einvoice.dto.EinvoiceSyncResponse;
import com.gst_reconsilation.einvoice.entity.EinvoiceFiling;
import com.gst_reconsilation.einvoice.entity.EinvoiceIrn;
import com.gst_reconsilation.einvoice.repository.EinvoiceFilingRepository;
import com.gst_reconsilation.einvoice.repository.EinvoiceIrnRepository;
import com.gst_reconsilation.einvoice.service.EinvoiceSyncService;
import com.gst_reconsilation.gstr1.entity.Gstr1B2b;
import com.gst_reconsilation.gstr1.entity.Gstr1EinvoiceReconciliationResult;
import com.gst_reconsilation.gstr1.entity.Gstr1Filing;
import com.gst_reconsilation.gstr1.repository.Gstr1B2bRepository;
import com.gst_reconsilation.gstr1.repository.Gstr1EinvoiceReconciliationResultRepository;
import com.gst_reconsilation.gstr1.repository.Gstr1FilingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reconciles the outward-supply sale register (GSTR-1 B2B) against e-invoice IRN
 * records for the same GSTIN + tax period.
 *
 * IMPORTANT NAMING NOTE: in {@link EinvoiceIrn}, the field {@code supplierGstin}
 * actually holds the government API's "ctin" — the COUNTERPARTY GSTIN — because
 * that's how it's populated when syncing IRNs for our own GSTIN (we're always the
 * supplier when raising a B2B e-invoice, so "ctin" in that response = the buyer).
 * That's why it's matched here against {@code Gstr1B2b.gstinOfRecipient}, not against
 * our own company GSTIN. Worth renaming later for clarity, but left as-is to avoid
 * touching the already-working einvoice module's schema.
 */
@Service
@RequiredArgsConstructor
public class Gstr1EinvoiceReconciliationService {

    private final Gstr1FilingRepository filingRepository;
    private final Gstr1B2bRepository b2bRepository;
    private final Gstr1EinvoiceReconciliationResultRepository resultRepository;
    private final EinvoiceFilingRepository einvoiceFilingRepository;
    private final EinvoiceIrnRepository einvoiceIrnRepository;
    private final EinvoiceSyncService einvoiceSyncService;

    private static final Map<String, String> MONTH_NUM = Map.ofEntries(
            Map.entry("JANUARY", "01"), Map.entry("FEBRUARY", "02"), Map.entry("MARCH", "03"),
            Map.entry("APRIL", "04"), Map.entry("MAY", "05"), Map.entry("JUNE", "06"),
            Map.entry("JULY", "07"), Map.entry("AUGUST", "08"), Map.entry("SEPTEMBER", "09"),
            Map.entry("OCTOBER", "10"), Map.entry("NOVEMBER", "11"), Map.entry("DECEMBER", "12")
    );

    /**
     * The "Sync" button: pulls e-invoice IRNs for this filing's GSTIN + derived period
     * (upsert-by-IRN under the hood — see {@link EinvoiceSyncService}, so already-paired
     * rows and already-known IRNs are never duplicated or wiped), then re-runs the match.
     */
    @Transactional
    public Map<String, Object> syncAndReconcile(Integer gstr1FilingId, Integer userId) {
        Gstr1Filing filing = filingRepository.findById(gstr1FilingId)
                .orElseThrow(() -> new RuntimeException("Gstr1Filing not found: " + gstr1FilingId));

        String retPeriod = deriveRetPeriod(filing.getFinancialYear(), filing.getTaxPeriod());

        EinvoiceSyncRequest req = new EinvoiceSyncRequest();
        req.setCompanyGstId(filing.getCompanyGST().getId());
        req.setRetPeriod(retPeriod);

        EinvoiceSyncResponse syncResult = einvoiceSyncService.sync(req, userId);
        List<Gstr1EinvoiceReconciliationResult> results = reconcile(filing);

        Map<String, Object> summary = new HashMap<>();
        summary.put("retPeriod", retPeriod);
        summary.put("einvoiceRowsSynced", syncResult.getIrnRowsSynced());
        summary.put("reconciledCount", results.size());
        return summary;
    }

    /** Matches sale-register rows against e-invoice IRN rows, updates isPaired on both sides,
     *  and rebuilds the persisted reconciliation result for this filing. */
    @Transactional
    public List<Gstr1EinvoiceReconciliationResult> reconcile(Gstr1Filing filing) {
        Integer gstr1FilingId = filing.getId();
        String retPeriod = deriveRetPeriod(filing.getFinancialYear(), filing.getTaxPeriod());

        EinvoiceFiling einvoiceFiling = einvoiceFilingRepository
                .findByCompanyGST_IdAndRetPeriod(filing.getCompanyGST().getId(), retPeriod)
                .orElseThrow(() -> new RuntimeException(
                        "No e-invoice data synced yet for period " + retPeriod + " — click Sync first"));

        List<Gstr1B2b> saleRegisterRows = b2bRepository.findByFiling_Id(gstr1FilingId);

        // Only match against regular B2B invoices — SEZ/Export supplyType and
        // credit/debit note docType don't belong in a 1:1 comparison against
        // Gstr1B2b (which only holds regular sale-register invoices).
        List<EinvoiceIrn> einvoiceRows = einvoiceIrnRepository.findByFiling_Id(einvoiceFiling.getId())
                .stream()
                .filter(r -> "B2B".equalsIgnoreCase(r.getSupplyType()) && "INV".equalsIgnoreCase(r.getDocType()))
                .collect(Collectors.toList());

        Map<String, Gstr1B2b> srMap = new HashMap<>();
        for (Gstr1B2b r : saleRegisterRows) srMap.put(key(r.getGstinOfRecipient(), r.getInvoiceNumber()), r);

        Map<String, EinvoiceIrn> eiMap = new HashMap<>();
        for (EinvoiceIrn r : einvoiceRows) eiMap.put(key(r.getSupplierGstin(), r.getDocNum()), r);

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(srMap.keySet());
        allKeys.addAll(eiMap.keySet());

        resultRepository.deleteByFiling_Id(gstr1FilingId);

        List<Gstr1EinvoiceReconciliationResult> results = new ArrayList<>();
        List<Gstr1B2b> b2bToUpdate = new ArrayList<>();
        List<EinvoiceIrn> irnToUpdate = new ArrayList<>();

        for (String k : allKeys) {
            Gstr1B2b sr = srMap.get(k);
            EinvoiceIrn ei = eiMap.get(k);
            String status;

            if (sr != null && ei != null) {
                boolean valueMatch = nz(sr.getInvoiceValue()).compareTo(nz(ei.getTotInvAmt())) == 0;
                status = valueMatch ? "MATCHED" : "VALUE_MISMATCH";
                sr.setIsPaired(true);
                sr.setPairedIrn(ei.getIrn());
                ei.setIsPaired(true);
                ei.setPairedGstr1B2bId(sr.getId());
                b2bToUpdate.add(sr);
                irnToUpdate.add(ei);
            } else if (sr != null) {
                status = "IN_SALE_REGISTER_ONLY"; // booked, no e-invoice raised yet
                sr.setIsPaired(false);
                sr.setPairedIrn(null);
                b2bToUpdate.add(sr);
            } else {
                status = "IN_EINVOICE_ONLY"; // e-invoiced, missing from the uploaded register
                ei.setIsPaired(false);
                ei.setPairedGstr1B2bId(null);
                irnToUpdate.add(ei);
            }

            results.add(Gstr1EinvoiceReconciliationResult.builder()
                    .filing(filing)
                    .recipientGstin(sr != null ? sr.getGstinOfRecipient() : ei.getSupplierGstin())
                    .invoiceNumber(sr != null ? sr.getInvoiceNumber() : ei.getDocNum())
                    .matchStatus(status)
                    .saleRegisterInvoiceValue(sr != null ? sr.getInvoiceValue() : null)
                    .saleRegisterTaxableValue(sr != null ? sr.getTaxableValue() : null)
                    .einvoiceInvoiceValue(ei != null ? ei.getTotInvAmt() : null)
                    .einvoiceIrn(ei != null ? ei.getIrn() : null)
                    .einvoiceStatus(ei != null ? ei.getIrnStatus() : null)
                    .build());
        }

        b2bRepository.saveAll(b2bToUpdate);
        einvoiceIrnRepository.saveAll(irnToUpdate);
        return resultRepository.saveAll(results);
    }

    /** "Link" button — sale-register invoices with no e-invoice yet, i.e. what still needs
     *  to be raised on the government portal. */
    public List<Gstr1EinvoiceReconciliationResult> getUnlinked(Integer gstr1FilingId) {
        return resultRepository.findByFiling_IdAndMatchStatus(gstr1FilingId, "IN_SALE_REGISTER_ONLY");
    }

    public List<Gstr1EinvoiceReconciliationResult> getResult(Integer gstr1FilingId, String matchStatus) {
        return matchStatus != null
                ? resultRepository.findByFiling_IdAndMatchStatus(gstr1FilingId, matchStatus)
                : resultRepository.findByFiling_Id(gstr1FilingId);
    }

    public List<EinvoiceIrn> getEinvoicesForFiling(Gstr1Filing filing) {
        String retPeriod = deriveRetPeriod(filing.getFinancialYear(), filing.getTaxPeriod());
        return einvoiceFilingRepository.findByCompanyGST_IdAndRetPeriod(filing.getCompanyGST().getId(), retPeriod)
                .map(ef -> einvoiceIrnRepository.findByFiling_Id(ef.getId()))
                .orElse(List.of());
    }

    /** "2025-26" + "October" -> "102025" (MMYYYY, matching the government API's retperiod format). */
    private String deriveRetPeriod(String financialYear, String taxPeriod) {
        String mm = MONTH_NUM.get(taxPeriod.trim().toUpperCase());
        if (mm == null) throw new RuntimeException("Unrecognized tax period: " + taxPeriod);
        String[] parts = financialYear.split("-");
        String startYear = parts[0];
        String endYear = startYear.substring(0, 2) + parts[1];
        String yyyy = Integer.parseInt(mm) >= 4 ? startYear : endYear; // Apr-Dec = start FY year, Jan-Mar = end FY year
        return mm + yyyy;
    }

    private String key(String gstin, String invNo) {
        return (gstin == null ? "" : gstin.toUpperCase()) + "|" + (invNo == null ? "" : invNo.trim().toUpperCase());
    }

    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}