package com.gst_reconsilation.gstr1.service;

import com.gst_reconsilation.ewaybill.entity.EwaybillFiling;
import com.gst_reconsilation.ewaybill.entity.EwaybillRecord;
import com.gst_reconsilation.ewaybill.repository.EwaybillRecordRepository;
import com.gst_reconsilation.gstr1.entity.Gstr1B2b;
import com.gst_reconsilation.gstr1.entity.Gstr1EwaybillReconciliationResult;
import com.gst_reconsilation.gstr1.entity.Gstr1Filing;
import com.gst_reconsilation.gstr1.repository.Gstr1B2bRepository;
import com.gst_reconsilation.gstr1.repository.Gstr1EwaybillReconciliationResultRepository;
import com.gst_reconsilation.gstr1.repository.Gstr1FilingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reconciles the outward-supply sale register (GSTR-1 B2B) against e-way bill records for the
 * same GSTIN + tax period.
 *
 * Unlike e-invoice reconciliation (a strict 1:1 key match), GSTR-1 invoices and e-way bills are
 * many-to-many: a single e-way bill can cover multiple invoices (e.g. one e-way bill of ₹1,30,000
 * covering a ₹70,000 invoice and a ₹60,000 invoice shipped together). {@link EwaybillRecord} only
 * stores a flat {@code docNo} string per e-way bill (no structured invoice list), so that field is
 * treated as a comma/semicolon-separated list of the invoice number(s) it covers — NOT split on
 * "/", since Indian GST invoice numbers routinely contain "/" as part of the number itself
 * (e.g. "MSP/2025/26-049").
 *
 * For each e-way bill, every matched invoice's value is subtracted from the e-way bill's
 * totalValue to leave a "bucket remainder" — zero means the bucket is fully accounted for
 * (MATCHED); a non-zero remainder or an unmatched docNo token means something is missing
 * (VALUE_MISMATCH).
 */
@Service
@RequiredArgsConstructor
public class Gstr1EwaybillReconciliationService {

    private final Gstr1FilingRepository filingRepository;
    private final Gstr1B2bRepository b2bRepository;
    private final Gstr1EwaybillReconciliationResultRepository resultRepository;
    private final EwaybillRecordRepository ewaybillRecordRepository;

    private static final Map<String, Integer> MONTH_NUM = Map.ofEntries(
            Map.entry("JANUARY", 1), Map.entry("FEBRUARY", 2), Map.entry("MARCH", 3),
            Map.entry("APRIL", 4), Map.entry("MAY", 5), Map.entry("JUNE", 6),
            Map.entry("JULY", 7), Map.entry("AUGUST", 8), Map.entry("SEPTEMBER", 9),
            Map.entry("OCTOBER", 10), Map.entry("NOVEMBER", 11), Map.entry("DECEMBER", 12)
    );

    /**
     * Both date shapes are present in {@code ewaybill_record}, so both have to parse here:
     * the Excel-upload path normalizes to dd-MM-yyyy, while the sync path stores the government
     * API's own strings verbatim ("dd/MM/yyyy HH:mm:ss a" — see {@code EwaybillExcelParserService}).
     * Parsing only dd-MM-yyyy silently dropped every synced e-way bill from the period filter,
     * which left the whole sale register looking IN_SALE_REGISTER_ONLY.
     */
    private static final List<DateTimeFormatter> DATE_FMTS = List.of(
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    /** Re-runs the bucket match for this filing against whatever e-way bill data already exists
     *  (synced or Excel-uploaded via the E-Way Bill page) for the filing's GSTIN + period. */
    @Transactional
    public List<Gstr1EwaybillReconciliationResult> reconcile(Integer gstr1FilingId) {
        Gstr1Filing filing = filingRepository.findById(gstr1FilingId)
                .orElseThrow(() -> new RuntimeException("Gstr1Filing not found: " + gstr1FilingId));

        int[] ym = derivePeriodYearMonth(filing.getFinancialYear(), filing.getTaxPeriod());

        List<Gstr1B2b> saleRegisterRows = b2bRepository.findByFiling_Id(gstr1FilingId);

        List<EwaybillRecord> periodEwbRecords = ewaybillRecordRepository
                .findByFiling_CompanyGST_Id(filing.getCompanyGST().getId())
                .stream()
                .filter(r -> matchesPeriod(r, ym[0], ym[1]))
                .collect(Collectors.toList());

        Map<String, Gstr1B2b> invoiceMap = new HashMap<>();
        for (Gstr1B2b b : saleRegisterRows) invoiceMap.put(norm(b.getInvoiceNumber()), b);

        resultRepository.deleteByFiling_Id(gstr1FilingId);

        Set<Integer> consumedB2bIds = new HashSet<>();
        List<Gstr1EwaybillReconciliationResult> results = new ArrayList<>();
        List<Gstr1B2b> b2bToUpdate = new ArrayList<>();

        for (EwaybillRecord ewb : periodEwbRecords) {
            List<String> tokens = splitDocNo(ewb.getDocNo());
            List<Gstr1B2b> matched = new ArrayList<>();
            boolean hasUnmatchedToken = false;

            for (String t : tokens) {
                Gstr1B2b b = invoiceMap.get(norm(t));
                if (b != null) {
                    matched.add(b);
                    consumedB2bIds.add(b.getId());
                } else {
                    hasUnmatchedToken = true;
                }
            }

            BigDecimal sumMatched = matched.stream()
                    .map(Gstr1B2b::getInvoiceValue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // totalValue is the taxable value while Gstr1B2b.invoiceValue is tax-inclusive, so
            // comparing the two left the bucket remainder permanently non-zero and every
            // correctly-covered e-way bill stuck in VALUE_MISMATCH. totInvValue is the
            // like-for-like figure; fall back to totalValue only when it was never populated.
            BigDecimal ewbValue = ewb.getTotInvValue() != null ? ewb.getTotInvValue() : nz(ewb.getTotalValue());
            BigDecimal diff = ewbValue.subtract(sumMatched);

            String status;
            if (matched.isEmpty()) {
                status = "IN_EWAYBILL_ONLY";
            } else if (!hasUnmatchedToken && diff.compareTo(BigDecimal.ZERO) == 0) {
                status = "MATCHED";
            } else {
                status = "VALUE_MISMATCH";
            }

            for (Gstr1B2b b : matched) {
                b.setPairedEwbNo(ewb.getEwbNo());
                b2bToUpdate.add(b);
            }

            results.add(Gstr1EwaybillReconciliationResult.builder()
                    .filing(filing)
                    .ewbNo(ewb.getEwbNo())
                    .docNo(ewb.getDocNo())
                    .recipientGstin(ewb.getToGstin())
                    .invoiceNumbers(matched.stream().map(Gstr1B2b::getInvoiceNumber).collect(Collectors.joining(", ")))
                    .ewaybillValue(ewbValue)
                    .saleRegisterValue(sumMatched)
                    .difference(diff)
                    .matchStatus(status)
                    .build());
        }

        // Sale-register invoices no e-way bill's docNo ever referenced
        for (Gstr1B2b b : saleRegisterRows) {
            if (consumedB2bIds.contains(b.getId())) continue;
            b.setPairedEwbNo(null);
            b2bToUpdate.add(b);
            results.add(Gstr1EwaybillReconciliationResult.builder()
                    .filing(filing)
                    .recipientGstin(b.getGstinOfRecipient())
                    .invoiceNumbers(b.getInvoiceNumber())
                    .saleRegisterValue(b.getInvoiceValue())
                    .difference(b.getInvoiceValue() != null ? b.getInvoiceValue().negate() : null)
                    .matchStatus("IN_SALE_REGISTER_ONLY")
                    .build());
        }

        b2bRepository.saveAll(b2bToUpdate);
        return resultRepository.saveAll(results);
    }

    public List<Gstr1EwaybillReconciliationResult> getResult(Integer gstr1FilingId, String matchStatus) {
        return matchStatus != null
                ? resultRepository.findByFiling_IdAndMatchStatus(gstr1FilingId, matchStatus)
                : resultRepository.findByFiling_Id(gstr1FilingId);
    }

    /** Sale-register invoices with no e-way bill covering them — what still needs one raised. */
    public List<Gstr1EwaybillReconciliationResult> getUnlinked(Integer gstr1FilingId) {
        return resultRepository.findByFiling_IdAndMatchStatus(gstr1FilingId, "IN_SALE_REGISTER_ONLY");
    }

    /** Splits on comma/semicolon only — never "/", which is routinely part of a single invoice number. */
    private List<String> splitDocNo(String docNo) {
        if (docNo == null || docNo.isBlank()) return List.of();
        return Arrays.stream(docNo.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * A bill counts for the period if its own document date says so, or — when that date is missing,
     * unparseable, or simply disagrees — if it was imported under that period's filing.
     *
     * Judging by the record date alone silently dropped every e-way bill the E-Way Bill page lists
     * for the period, so the whole sale register came back IN_SALE_REGISTER_ONLY and re-running
     * Reconcile could never change that: the same filter skipped the same rows every time. Bills are
     * imported per sync-date filing, so that filing is the accountant's own statement of which period
     * they belong to, and is trusted here as a fallback.
     */
    private boolean matchesPeriod(EwaybillRecord r, int year, int month) {
        LocalDate d = parseDate(r.getDocDate());
        if (d == null) d = parseDate(r.getEwbDate());
        if (d != null && d.getYear() == year && d.getMonthValue() == month) return true;
        return filingCoversPeriod(r.getFiling(), year, month);
    }

    /** syncDate is a free-form string — "2026-02-01" is what the app writes, "01/02/2026" is what
     *  this entity's own contract documents — so lean on parseDate rather than assuming one shape. */
    private boolean filingCoversPeriod(EwaybillFiling f, int year, int month) {
        if (f == null) return false;
        LocalDate d = parseDate(f.getSyncDate());
        return d != null && d.getYear() == year && d.getMonthValue() == month;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        // Drop any trailing time component ("15/02/2026 03:30:00 PM") before parsing the date.
        String datePart = s.trim().split("\\s+")[0];
        for (DateTimeFormatter fmt : DATE_FMTS) {
            try {
                return LocalDate.parse(datePart, fmt);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** "2025-26" + "October" -> [2025, 10]; Jan-Mar fall in the FY's second calendar year. */
    private int[] derivePeriodYearMonth(String financialYear, String taxPeriod) {
        Integer month = MONTH_NUM.get(taxPeriod.trim().toUpperCase());
        if (month == null) throw new RuntimeException("Unrecognized tax period: " + taxPeriod);
        int startYear = Integer.parseInt(financialYear.split("-")[0]);
        int year = month >= 4 ? startYear : startYear + 1;
        return new int[]{year, month};
    }

    private String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
