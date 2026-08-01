// ims/service/ImsExcelParserService.java
package com.gst_reconsilation.ims.service;

import com.gst_reconsilation.ims.entity.ImsFiling;
import com.gst_reconsilation.ims.entity.ImsInvoice;
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
 * Parses the IMS_Manual_Entry_Template_V1.xlsx workbook (sheet "ims_data").
 * Unlike the nested B2B/B2BA/CDNR/... JSON shape the live IMS API returns, this
 * template exposes ONE flat sheet with a "Section" column the user picks from a
 * dropdown — replicating 8 separate government-JSON-shaped sheets for manual
 * entry would be needlessly hostile to a human filling this in by hand.
 * Row 4 is the header row, data starts row 5 — same DATA_START convention as
 * every other Excel parser in this codebase (Gstr1, Gstr2).
 */
@Service
public class ImsExcelParserService {

    private static final int DATA_START = 4;
    private static final String SHEET_NAME = "ims_data";

    public List<ImsInvoice> parse(InputStream is, ImsFiling filing) throws Exception {
        List<ImsInvoice> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheet(SHEET_NAME);
            if (sheet == null) return rows;
            for (int i = DATA_START; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                ImsInvoice inv = buildRow(row, filing);
                if (inv != null) rows.add(inv);
            }
        }
        return rows;
    }

    // Section, supplierGstin, recipientGstin, ecommerceGstin, origInvNo, origInvDate,
    // invNo, invDate, invValue, pos, invType, reverseCharge, rate, taxableValue,
    // igst, cgst, sgst, cess, imsAction, remarks
    private ImsInvoice buildRow(Row r, ImsFiling f) {
        String section = s(r, 0), invNo = s(r, 6);
        if (blank(section) || blank(invNo)) return null;
        return ImsInvoice.builder().filing(f).createdBy(f.getCreatedBy())
                .section(section.toUpperCase())
                .supplierGstin(upper(s(r, 1))).recipientGstin(upper(s(r, 2))).ecommerceGstin(upper(s(r, 3)))
                .originalInvoiceNumber(s(r, 4)).originalInvoiceDate(d(r, 5))
                .invoiceNumber(invNo).invoiceDate(d(r, 7)).invoiceValue(n(r, 8))
                .placeOfSupply(s(r, 9)).invoiceType(s(r, 10)).reverseCharge(s(r, 11))
                .rate(n(r, 12)).taxableValue(z(n(r, 13)))
                .integratedTax(z(n(r, 14))).centralTax(z(n(r, 15))).stateUtTax(z(n(r, 16))).cess(z(n(r, 17)))
                .imsAction(s(r, 18)).remarks(s(r, 19))
                .source("EXCEL")
                .build();
    }

    // ── cell helpers (identical pattern to ExcelParserService / Gstr2ExcelParserService) ──

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

    private LocalDate d(Row row, int col) {
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c))
            return c.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (c.getCellType() == CellType.STRING) {
            String sv = c.getStringCellValue().trim();
            for (DateTimeFormatter fmt : DATE_FMTS) {
                try { return LocalDate.parse(sv, fmt); } catch (Exception ignored) {}
            }
        }
        return null;
    }

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