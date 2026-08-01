// einvoice/service/EinvoiceExcelParserService.java
package com.gst_reconsilation.einvoice.service;

import com.gst_reconsilation.einvoice.entity.EinvoiceFiling;
import com.gst_reconsilation.einvoice.entity.EinvoiceIrn;
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
 * Parses the EInvoice_Manual_Entry_Template_V2.xlsx workbook (sheet "einvoice_data").
 * Row 4 is the header row, data starts row 5 — same DATA_START convention as
 * every other Excel parser in this codebase.
 */
@Service
public class EinvoiceExcelParserService {

    private static final int DATA_START = 4;
    private static final String SHEET_NAME = "einvoice_data";

    public List<EinvoiceIrn> parse(InputStream is, EinvoiceFiling filing, Integer userId) throws Exception {
        List<EinvoiceIrn> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheet(SHEET_NAME);
            if (sheet == null) return rows;
            for (int i = DATA_START; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                EinvoiceIrn irn = buildRow(row, filing, userId);
                if (irn != null) rows.add(irn);
            }
        }
        return rows;
    }

    // 0 supplierGstin, 1 irn, 2 ackNo, 3 ackDt, 4 docNum, 5 docDate, 6 docType, 7 supplyType,
    // 8 totInvAmt, 9 irnStatus, 10 ewbNo, 11 ewbDt, 12 ewbValidTill,
    // 13 cnlDt, 14 cnlRsn, 15 cnlRem, 16 remarks
    private EinvoiceIrn buildRow(Row r, EinvoiceFiling filing, Integer userId) {
        String supplierGstin = s(r, 0), irnVal = s(r, 1);
        if (blank(supplierGstin) || blank(irnVal)) return null;

        return EinvoiceIrn.builder()
                .filing(filing).createdBy(userId)
                .supplierGstin(upper(supplierGstin))
                .irn(irnVal)
                .ackNo(longVal(n(r, 2)))
                .ackDt(dateStr(r, 3))
                .docNum(s(r, 4))
                .docDate(dateStr(r, 5))
                .docType(s(r, 6))
                .supplyType(s(r, 7))
                .totInvAmt(n(r, 8))
                .irnStatus(s(r, 9))
                .ewbNo(longVal(n(r, 10)))
                .ewbDt(dateStr(r, 11))
                .ewbValidTill(dateStr(r, 12))
                .cnlDt(dateStr(r, 13))
                .cnlRsn(s(r, 14))
                .cnlRem(s(r, 15))
                .remarks(s(r, 16))
                .source("EXCEL")
                .build();
    }

    // ── cell helpers (identical pattern to ImsExcelParserService / EwaybillExcelParserService) ──

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

    /** EinvoiceIrn stores dates as plain strings (matching the API's own format), so
     *  this normalizes to dd-MM-yyyy rather than returning a LocalDate. */
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
            return sv;
        }
        return null;
    }

    private Long longVal(BigDecimal v) { return v == null ? null : v.longValue(); }
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