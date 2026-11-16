package com.gst_reconsilation.whitebooks;

import java.util.Locale;
import java.util.Map;

/** Converts the app's (financialYear, taxPeriod) pair into the GSTN return period "MMYYYY". */
public final class GstPeriods {

    private GstPeriods() {}

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("JAN", 1), Map.entry("FEB", 2), Map.entry("MAR", 3), Map.entry("APR", 4),
            Map.entry("MAY", 5), Map.entry("JUN", 6), Map.entry("JUL", 7), Map.entry("AUG", 8),
            Map.entry("SEP", 9), Map.entry("OCT", 10), Map.entry("NOV", 11), Map.entry("DEC", 12));

    /**
     * @param financialYear e.g. "2026-27" (Apr-Dec fall in 2026, Jan-Mar in 2027)
     * @param taxPeriod     month name ("JUNE", "June", "Jun"), month number ("06"), or already "MMYYYY"
     */
    public static String toRetPeriod(String financialYear, String taxPeriod) {
        if (taxPeriod == null || taxPeriod.isBlank()) {
            throw new IllegalArgumentException("Tax period is required");
        }
        String p = taxPeriod.trim();
        if (p.matches("\\d{6}")) return p;

        int month;
        if (p.matches("\\d{1,2}")) {
            month = Integer.parseInt(p);
        } else {
            String key = p.length() >= 3 ? p.substring(0, 3).toUpperCase(Locale.ROOT) : "";
            Integer m = MONTHS.get(key);
            if (m == null) throw new IllegalArgumentException("Unrecognised tax period: " + taxPeriod);
            month = m;
        }
        if (month < 1 || month > 12) throw new IllegalArgumentException("Unrecognised tax period: " + taxPeriod);

        if (financialYear == null || !financialYear.trim().matches("\\d{4}.*")) {
            throw new IllegalArgumentException("Financial year must look like 2026-27, got: " + financialYear);
        }
        int startYear = Integer.parseInt(financialYear.trim().substring(0, 4));
        int year = month <= 3 ? startYear + 1 : startYear;
        return String.format("%02d%d", month, year);
    }
}
