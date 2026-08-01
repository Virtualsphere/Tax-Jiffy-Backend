// ewaybill/service/EwaybillExcelParserService.java
package com.gst_reconsilation.ewaybill.service;

import com.gst_reconsilation.ewaybill.entity.EwaybillFiling;
import com.gst_reconsilation.ewaybill.entity.EwaybillRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the EWayBill_Manual_Entry_Template_V2.xlsx workbook (sheet "ewaybill_data").
 * Row 4 is the header row, data starts row 5 — same DATA_START convention as
 * every other Excel parser in this codebase.
 */
@Service
public class EwaybillExcelParserService {

    private static final int DATA_START = 4;
    private static final String SHEET_NAME = "ewaybill_data";

    public List<EwaybillRecord> parse(InputStream is, EwaybillFiling filing, Integer userId) throws Exception {
        List<EwaybillRecord> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheet(SHEET_NAME);
            if (sheet == null) return rows;
            for (int i = DATA_START; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                EwaybillRecord rec = buildRow(row, filing, userId);
                if (rec != null) rows.add(rec);
            }
        }
        return rows;
    }

    // 0 ewbNo, 1 ewbDate, 2 genMode, 3 userGstin, 4 supplyType, 5 subSupplyType,
    // 6 docType, 7 docNo, 8 docDate, 9 fromGstin, 10 fromTrdName, 11 fromPlace,
    // 12 fromPincode, 13 fromStateCode, 14 toGstin, 15 toTrdName, 16 toPlace,
    // 17 toPincode, 18 toStateCode, 19 totalValue, 20 totInvValue, 21 cgstValue,
    // 22 sgstValue, 23 igstValue, 24 cessValue, 25 transporterId, 26 transporterName,
    // 27 status, 28 validUpto, 29 extendedTimes, 30 rejectStatus
    private EwaybillRecord buildRow(Row r, EwaybillFiling filing, Integer userId) {
        BigDecimal ewbNoVal = n(r, 0);
        String docNo = s(r, 7);
        if (ewbNoVal == null || blank(docNo)) return null;

        return EwaybillRecord.builder()
                .filing(filing).createdBy(userId)
                .ewbNo(ewbNoVal.longValue())
                .ewbDate(dateStr(r, 1))
                .genMode(s(r, 2)).userGstin(upper(s(r, 3)))
                .supplyType(s(r, 4)).subSupplyType(s(r, 5))
                .docType(s(r, 6)).docNo(docNo).docDate(dateStr(r, 8))
                .fromGstin(upper(s(r, 9))).fromTrdName(s(r, 10)).fromPlace(s(r, 11))
                .fromPincode(intVal(n(r, 12))).fromStateCode(intVal(n(r, 13)))
                .toGstin(upper(s(r, 14))).toTrdName(s(r, 15)).toPlace(s(r, 16))
                .toPincode(intVal(n(r, 17))).toStateCode(intVal(n(r, 18)))
                .totalValue(z(n(r, 19))).totInvValue(z(n(r, 20)))
                .cgstValue(z(n(r, 21))).sgstValue(z(n(r, 22)))
                .igstValue(z(n(r, 23))).cessValue(z(n(r, 24)))
                .transporterId(s(r, 25)).transporterName(s(r, 26))
                .status(s(r, 27)).validUpto(dateStr(r, 28))
                .extendedTimes(intVal(n(r, 29))).rejectStatus(s(r, 30))
                .source("EXCEL")
                .build();
    }

    // ── cell helpers (identical pattern to ImsExcelParserService) ──

    private String s(Row row, int col) {
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().trim();
            case NUMERIC -> { double v = c.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v); }
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            case FORMULA -> { try { yield c.getRichStringCellValue().getString().trim(); }
            catch (Exception e) { yield String.valueOf(c.getNumericCellValue()); } }
            default -> null;
        };
    }

    private BigDecimal n(Row row, int col) {
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(c.getNumericCellValue());
            case STRING  -> { try { yield new BigDecimal(c.getStringCellValue().trim()); }
            catch (NumberFormatException e) { yield null; } }
            case FORMULA -> { try { yield BigDecimal.valueOf(c.getNumericCellValue()); }
            catch (Exception e) { yield null; } }
            default -> null;
        };
    }

    private static final List<DateTimeFormatter> DATE_FMTS = List.of(
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    /** E-way bill dates are stored as plain strings on the entity (to match the API's own
     *  "dd/MM/yyyy HH:mm:ss a" format), so this returns a normalized dd-MM-yyyy string,
     *  not a LocalDate — consistent with how the sync path stores dates today. */
    private String dateStr(Row row, int col) {
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            LocalDate ld = c.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return ld.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        }
        if (c.getCellType() == CellType.STRING) {
            String sv = c.getStringCellValue().trim();
            for (DateTimeFormatter fmt : DATE_FMTS) {
                try {
                    LocalDate ld = LocalDate.parse(sv, fmt);
                    return ld.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                } catch (Exception ignored) {}
            }
            return sv; // fall back to raw text rather than dropping it
        }
        return null;
    }

    private Integer intVal(BigDecimal v) { return v == null ? null : v.intValue(); }
    private BigDecimal z(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private boolean blank(String s) { return s == null || s.isBlank(); }
    private String upper(String s) { return s != null ? s.toUpperCase() : null; }

    private boolean isRowEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell c = row.getCell(i);
            if (c != null && c.getCellType() != CellType.BLANK) {
                String v = s(row, i);
                if (v != null && !v.isBlank()) return false;
            }
        }
        return true;
    }
}