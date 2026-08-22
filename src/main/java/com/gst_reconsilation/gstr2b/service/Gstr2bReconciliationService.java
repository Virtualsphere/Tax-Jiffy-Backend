// gstr2b/service/Gstr2bReconciliationService.java
package com.gst_reconsilation.gstr2b.service;

import com.gst_reconsilation.gstr2b.dto.Gstr2bInvoiceUpdateRequest;
import com.gst_reconsilation.gstr2b.dto.Gstr2bReconciliationRow;
import com.gst_reconsilation.gstr3b.entity.Gstr2B2b;
import com.gst_reconsilation.gstr3b.entity.Gstr2Filing;
import com.gst_reconsilation.gstr3b.repository.Gstr2B2bRepository;
import com.gst_reconsilation.gstr3b.repository.Gstr2FilingRepository;
import com.gst_reconsilation.ims.entity.ImsInvoice;
import com.gst_reconsilation.ims.repository.ImsFilingRepository;
import com.gst_reconsilation.ims.service.ImsSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Reconciles the uploaded GSTR-2B data — which in this system IS the purchase-register upload
 * ({@link Gstr2Filing} / {@link Gstr2B2b}, the same records behind the "Purchase Register" page —
 * there is no separate GSTR-2B entity or upload) against the IMS filing for the same
 * (companyGst, retperiod). Computed fresh on every call — nothing here is persisted except the
 * correction itself — so correcting a row is reflected the next time this runs, with no
 * separate re-sync step.
 *
 * Matching key is (supplierGstin, invoiceNumber), the same convention used by
 * Gstr1EinvoiceReconciliationService and Gstr3bReconciliationService elsewhere in this codebase.
 *
 * Bucket thresholds mirror the classify() logic from the reference reconciliation UI this was
 * built from: a per-head difference up to ₹1 is "rounding", and an invoice whose heads move in
 * opposite directions (IGST down, CGST+SGST up, or vice versa) while the total tax stays within
 * ₹5 is a place-of-supply mismatch — flagged separately because it is a blocked credit, not a
 * rounding artifact: IGST cannot be set off against a CGST/SGST entry.
 */
@Service
@RequiredArgsConstructor
public class Gstr2bReconciliationService {

    private static final BigDecimal PER_HEAD_TOLERANCE = new BigDecimal("1.00");
    private static final BigDecimal PER_INVOICE_TOLERANCE = new BigDecimal("5.00");
    private static final BigDecimal EPSILON = new BigDecimal("0.005");

    private static final Map<String, String> MONTH_TO_NUM = Map.ofEntries(
            Map.entry("JANUARY", "01"), Map.entry("FEBRUARY", "02"), Map.entry("MARCH", "03"),
            Map.entry("APRIL", "04"), Map.entry("MAY", "05"), Map.entry("JUNE", "06"),
            Map.entry("JULY", "07"), Map.entry("AUGUST", "08"), Map.entry("SEPTEMBER", "09"),
            Map.entry("OCTOBER", "10"), Map.entry("NOVEMBER", "11"), Map.entry("DECEMBER", "12")
    );

    private final Gstr2FilingRepository gstr2FilingRepository;
    private final Gstr2B2bRepository gstr2B2bRepository;
    private final ImsFilingRepository imsFilingRepository;
    private final ImsSyncService imsService;

    public List<Gstr2bReconciliationRow> reconcile(Integer gstr2FilingId) {
        Gstr2Filing filing = gstr2FilingRepository.findById(gstr2FilingId)
                .orElseThrow(() -> new RuntimeException("Gstr2Filing not found: " + gstr2FilingId));

        Map<String, Gstr2B2b> gstr2bRows = new LinkedHashMap<>();
        for (var r : gstr2B2bRepository.findByFiling_Id(gstr2FilingId)) {
            gstr2bRows.put(key(r.getGstinOfSupplier(), r.getInvoiceNumber()), r);
        }

        Map<String, ImsInvoice> imsRows = new LinkedHashMap<>();
        Integer companyGstId = filing.getCompanyGST() != null ? filing.getCompanyGST().getId() : null;
        String retPeriod = toRetPeriod(filing.getFinancialYear(), filing.getTaxPeriod());
        if (companyGstId != null && retPeriod != null) {
            imsFilingRepository.findByCompanyGST_IdAndRetPeriod(companyGstId, retPeriod)
                    .ifPresent(imsFiling -> {
                        for (var r : imsService.getByFiling(imsFiling.getId())) {
                            if ("B2B".equals(r.getSection())) {
                                imsRows.put(key(r.getSupplierGstin(), r.getInvoiceNumber()), r);
                            }
                        }
                    });
        }

        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(gstr2bRows.keySet());
        allKeys.addAll(imsRows.keySet());

        List<Gstr2bReconciliationRow> results = new ArrayList<>();
        for (String k : allKeys) {
            results.add(buildRow(gstr2bRows.get(k), imsRows.get(k)));
        }
        return results;
    }

    @Transactional
    public Gstr2B2b updateInvoice(Integer invoiceId, Gstr2bInvoiceUpdateRequest req) {
        Gstr2B2b inv = gstr2B2bRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("GSTR-2B invoice not found: " + invoiceId));
        if (inv.getFiling() != null && "FINALIZED".equals(inv.getFiling().getFilingStatus())) {
            throw new IllegalStateException("This period's GSTR-2B has already been finalized and can no longer be edited.");
        }

        boolean touched = false;
        if (req.getTaxableValue() != null && req.getTaxableValue().compareTo(inv.getTaxableValue()) != 0) {
            snapshotOnce(inv);
            inv.setTaxableValue(req.getTaxableValue());
            touched = true;
        }
        if (req.getRate() != null && (inv.getRate() == null || req.getRate().compareTo(inv.getRate()) != 0)) {
            snapshotOnce(inv);
            inv.setRate(req.getRate());
            touched = true;
        }
        if (req.getIntegratedTaxPaid() != null && req.getIntegratedTaxPaid().compareTo(nz(inv.getIntegratedTaxPaid())) != 0) {
            snapshotOnce(inv);
            inv.setIntegratedTaxPaid(req.getIntegratedTaxPaid());
            touched = true;
        }
        if (req.getCentralTaxPaid() != null && req.getCentralTaxPaid().compareTo(nz(inv.getCentralTaxPaid())) != 0) {
            snapshotOnce(inv);
            inv.setCentralTaxPaid(req.getCentralTaxPaid());
            touched = true;
        }
        if (req.getStateUtTaxPaid() != null && req.getStateUtTaxPaid().compareTo(nz(inv.getStateUtTaxPaid())) != 0) {
            snapshotOnce(inv);
            inv.setStateUtTaxPaid(req.getStateUtTaxPaid());
            touched = true;
        }
        if (req.getCessPaid() != null && req.getCessPaid().compareTo(nz(inv.getCessPaid())) != 0) {
            snapshotOnce(inv);
            inv.setCessPaid(req.getCessPaid());
            touched = true;
        }
        if (touched) inv.setEdited(true);

        if (req.getReconciliationAction() != null) inv.setReconciliationAction(req.getReconciliationAction());
        if (req.getRemarks() != null) inv.setReconciliationRemarks(req.getRemarks());

        return gstr2B2bRepository.save(inv);
    }

    /** Captures the as-uploaded value the first time any field on this row is corrected. */
    private void snapshotOnce(Gstr2B2b inv) {
        if (inv.isEdited()) return; // already snapshotted on a previous edit
        inv.setOriginalTaxableValue(inv.getTaxableValue());
        inv.setOriginalIntegratedTaxPaid(inv.getIntegratedTaxPaid());
        inv.setOriginalCentralTaxPaid(inv.getCentralTaxPaid());
        inv.setOriginalStateUtTaxPaid(inv.getStateUtTaxPaid());
        inv.setOriginalCessPaid(inv.getCessPaid());
    }

    @Transactional
    public Gstr2Filing finalizeFiling(Integer gstr2FilingId) {
        Gstr2Filing filing = gstr2FilingRepository.findById(gstr2FilingId)
                .orElseThrow(() -> new RuntimeException("Gstr2Filing not found: " + gstr2FilingId));
        filing.setFilingStatus("FINALIZED");
        return gstr2FilingRepository.save(filing);
    }

    private Gstr2bReconciliationRow buildRow(Gstr2B2b b, ImsInvoice i) {
        BigDecimal bTaxable = b != null ? nz(b.getTaxableValue()) : BigDecimal.ZERO;
        BigDecimal bIgst = b != null ? nz(b.getIntegratedTaxPaid()) : BigDecimal.ZERO;
        BigDecimal bCgst = b != null ? nz(b.getCentralTaxPaid()) : BigDecimal.ZERO;
        BigDecimal bSgst = b != null ? nz(b.getStateUtTaxPaid()) : BigDecimal.ZERO;
        BigDecimal bCess = b != null ? nz(b.getCessPaid()) : BigDecimal.ZERO;
        BigDecimal bTotal = bIgst.add(bCgst).add(bSgst).add(bCess);

        BigDecimal iTaxable = i != null ? nz(i.getTaxableValue()) : BigDecimal.ZERO;
        BigDecimal iIgst = i != null ? nz(i.getIntegratedTax()) : BigDecimal.ZERO;
        BigDecimal iCgst = i != null ? nz(i.getCentralTax()) : BigDecimal.ZERO;
        BigDecimal iSgst = i != null ? nz(i.getStateUtTax()) : BigDecimal.ZERO;
        BigDecimal iCess = i != null ? nz(i.getCess()) : BigDecimal.ZERO;
        BigDecimal iTotal = iIgst.add(iCgst).add(iSgst).add(iCess);

        BigDecimal dTaxable = round(bTaxable.subtract(iTaxable));
        BigDecimal dIgst = round(bIgst.subtract(iIgst));
        BigDecimal dCgst = round(bCgst.subtract(iCgst));
        BigDecimal dSgst = round(bSgst.subtract(iSgst));
        BigDecimal dCess = round(bCess.subtract(iCess));
        BigDecimal dTotal = round(bTotal.subtract(iTotal));

        BigDecimal atRisk = BigDecimal.ZERO, unclaimed = BigDecimal.ZERO;
        for (BigDecimal d : List.of(dIgst, dCgst, dSgst, dCess)) {
            if (d.compareTo(BigDecimal.ZERO) > 0) atRisk = atRisk.add(d);
            else unclaimed = unclaimed.add(d.abs());
        }

        String status = classify(b, i, dTaxable, dIgst, dCgst, dSgst, dCess, dTotal);

        return Gstr2bReconciliationRow.builder()
                .id((b != null ? b.getId() : "") + "|" + (i != null ? i.getId() : ""))
                .gstr2bInvoiceId(b != null ? b.getId() : null)
                .imsInvoiceId(i != null ? i.getId() : null)
                .supplierGstin(b != null ? b.getGstinOfSupplier() : (i != null ? i.getSupplierGstin() : null))
                .invoiceNumber(b != null ? b.getInvoiceNumber() : (i != null ? i.getInvoiceNumber() : null))
                .invoiceDate(b != null ? b.getInvoiceDate() : (i != null ? i.getInvoiceDate() : null))
                .itcAvailability(b != null ? b.getEligibilityForItc() : null)
                .matchStatus(status)
                .gstr2bTaxableValue(bTaxable).gstr2bIgst(bIgst).gstr2bCgst(bCgst).gstr2bSgst(bSgst).gstr2bCess(bCess)
                .gstr2bTotalTax(round(bTotal))
                .imsTaxableValue(iTaxable).imsIgst(iIgst).imsCgst(iCgst).imsSgst(iSgst).imsCess(iCess)
                .imsTotalTax(round(iTotal))
                .deltaTaxable(dTaxable).deltaIgst(dIgst).deltaCgst(dCgst).deltaSgst(dSgst).deltaCess(dCess)
                .deltaTotal(dTotal)
                .atRisk(round(atRisk)).unclaimed(round(unclaimed))
                .edited(b != null && b.isEdited())
                .reconciliationAction(b != null ? b.getReconciliationAction() : "PENDING")
                .remarks(b != null ? b.getReconciliationRemarks() : null)
                .build();
    }

    private String classify(Gstr2B2b b, ImsInvoice i,
                             BigDecimal dTaxable, BigDecimal dIgst, BigDecimal dCgst, BigDecimal dSgst, BigDecimal dCess,
                             BigDecimal dTotal) {
        if (b == null) return "ONLY_IN_IMS";
        if (i == null) return "ONLY_IN_2B";

        BigDecimal maxHeadDiff = List.of(dIgst, dCgst, dSgst, dCess).stream()
                .map(BigDecimal::abs).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        boolean up = List.of(dIgst, dCgst, dSgst, dCess).stream().anyMatch(v -> v.compareTo(EPSILON) > 0);
        boolean down = List.of(dIgst, dCgst, dSgst, dCess).stream().anyMatch(v -> v.compareTo(EPSILON.negate()) < 0);
        boolean sameTaxable = dTaxable.abs().compareTo(EPSILON) < 0;

        if (sameTaxable && maxHeadDiff.compareTo(EPSILON) < 0) return "MATCHED";
        if (sameTaxable && maxHeadDiff.compareTo(PER_HEAD_TOLERANCE) <= 0) return "ROUNDING_DIFFERENCE";
        if (sameTaxable && up && down && dTotal.abs().compareTo(PER_INVOICE_TOLERANCE) <= 0) return "TAX_HEAD_DIFFERS_POS";
        if (!sameTaxable) return "TAXABLE_VALUE_DIFFERS";
        return "TAX_AMOUNT_DIFFERS";
    }

    /** financialYear "2026-27" + taxPeriod "AUGUST" (case-insensitive) -> retPeriod "MMYYYY", same rule IMS/e-invoice use. */
    private String toRetPeriod(String financialYear, String taxPeriod) {
        if (financialYear == null || taxPeriod == null) return null;
        String monthNum = MONTH_TO_NUM.get(taxPeriod.trim().toUpperCase());
        if (monthNum == null) return null;
        try {
            int startYear = Integer.parseInt(financialYear.split("-")[0].trim());
            int actualYear = Integer.parseInt(monthNum) <= 3 ? startYear + 1 : startYear;
            return monthNum + actualYear;
        } catch (Exception e) {
            return null;
        }
    }

    private String key(String gstin, String invNo) {
        return (gstin == null ? "" : gstin.trim().toUpperCase()) + "|" + (invNo == null ? "" : invNo.trim().toUpperCase());
    }

    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private BigDecimal round(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }
}
