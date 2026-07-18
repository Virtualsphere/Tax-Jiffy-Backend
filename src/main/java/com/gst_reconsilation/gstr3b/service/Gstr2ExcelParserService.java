package com.gst_reconsilation.gstr3b.service;

import com.gst_reconsilation.gstr3b.entity.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Parses the official GSTR-2 Excel workbook (purchase return, part of GSTR-3B
 * reconciliation). Rows 0-3 are header/summary rows; actual data starts at row
 * index 4 - same layout convention as the GSTR-1 workbook.
 */
@Service
public class Gstr2ExcelParserService {

    private static final int DATA_START = 4;

    private static final String SH_B2B    = "b2b";
    private static final String SH_B2BUR  = "b2bur";
    private static final String SH_IMPS   = "imps";
    private static final String SH_IMPG   = "impg";
    private static final String SH_CDNR   = "cdnr";
    private static final String SH_CDNUR  = "cdnur";
    private static final String SH_AT     = "at";
    private static final String SH_ATADJ  = "atadj";
    private static final String SH_EXEMP  = "exemp";
    private static final String SH_ITCR   = "itcr";
    private static final String SH_HSNSUM = "hsnsum";

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────
    public ParseResult parse(InputStream is, Gstr2Filing filing) throws Exception {
        ParseResult r = new ParseResult();
        try (Workbook wb = new XSSFWorkbook(is)) {
            r.b2b    = parseSheet(wb, SH_B2B,    row -> buildB2b(row, filing));
            r.b2bur  = parseSheet(wb, SH_B2BUR,  row -> buildB2bur(row, filing));
            r.imps   = parseSheet(wb, SH_IMPS,   row -> buildImps(row, filing));
            r.impg   = parseSheet(wb, SH_IMPG,   row -> buildImpg(row, filing));
            r.cdnr   = parseSheet(wb, SH_CDNR,   row -> buildCdnr(row, filing));
            r.cdnur  = parseSheet(wb, SH_CDNUR,  row -> buildCdnur(row, filing));
            r.at     = parseSheet(wb, SH_AT,     row -> buildAt(row, filing));
            r.atadj  = parseSheet(wb, SH_ATADJ,  row -> buildAtadj(row, filing));
            r.exemp  = parseSheet(wb, SH_EXEMP,  row -> buildExemp(row, filing));
            r.itcr   = parseSheet(wb, SH_ITCR,   row -> buildItcr(row, filing));
            r.hsnSum = parseSheet(wb, SH_HSNSUM, row -> buildHsnSum(row, filing));
        }
        return r;
    }

    @FunctionalInterface
    private interface RowMapper<T> { T map(Row row); }

    private <T> List<T> parseSheet(Workbook wb, String name, RowMapper<T> mapper) {
        List<T> list = new ArrayList<>();
        Sheet sheet = wb.getSheet(name);
        if (sheet == null) return list;
        for (int i = DATA_START; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;
            T e = mapper.map(row);
            if (e != null) list.add(e);
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────
    //  ROW BUILDERS
    // ─────────────────────────────────────────────────────────────

    // b2b: gstin, invNo, invDate, invValue, pos, reverseCharge, invType, rate, taxVal,
    //      igst, cgst, sgst, cess, eligibility, availedIgst, availedCgst, availedSgst, availedCess
    private Gstr2B2b buildB2b(Row r, Gstr2Filing f) {
        String gstin = s(r, 0), inv = s(r, 1);
        if (blank(gstin) || blank(inv)) return null;
        return Gstr2B2b.builder().filing(f).createdBy(f.getCreatedBy())
                .gstinOfSupplier(gstin.toUpperCase()).invoiceNumber(inv).invoiceDate(d(r, 2))
                .invoiceValue(n(r, 3)).placeOfSupply(s(r, 4)).reverseCharge(s(r, 5))
                .invoiceType(s(r, 6)).rate(n(r, 7)).taxableValue(z(n(r, 8)))
                .integratedTaxPaid(z(n(r, 9))).centralTaxPaid(z(n(r, 10))).stateUtTaxPaid(z(n(r, 11)))
                .cessPaid(z(n(r, 12))).eligibilityForItc(s(r, 13))
                .availedItcIntegratedTax(z(n(r, 14))).availedItcCentralTax(z(n(r, 15)))
                .availedItcStateUtTax(z(n(r, 16))).availedItcCess(z(n(r, 17))).build();
    }

    // b2bur: supplierName, invNo, invDate, invValue, pos, supplyType, rate, taxVal,
    //        igst, cgst, sgst, cess, eligibility, availedIgst, availedCgst, availedSgst, availedCess
    private Gstr2B2bur buildB2bur(Row r, Gstr2Filing f) {
        String inv = s(r, 1);
        if (blank(inv)) return null;
        return Gstr2B2bur.builder().filing(f).createdBy(f.getCreatedBy())
                .supplierName(s(r, 0)).invoiceNumber(inv).invoiceDate(d(r, 2))
                .invoiceValue(n(r, 3)).placeOfSupply(s(r, 4)).supplyType(s(r, 5)).rate(n(r, 6))
                .taxableValue(z(n(r, 7))).integratedTaxPaid(z(n(r, 8))).centralTaxPaid(z(n(r, 9)))
                .stateUtTaxPaid(z(n(r, 10))).cessPaid(z(n(r, 11))).eligibilityForItc(s(r, 12))
                .availedItcIntegratedTax(z(n(r, 13))).availedItcCentralTax(z(n(r, 14)))
                .availedItcStateUtTax(z(n(r, 15))).availedItcCess(z(n(r, 16))).build();
    }

    // imps: invNoRegRecipient, invDate, invValue, pos, rate, taxVal, igst, cess,
    //       eligibility, availedIgst, availedCess
    private Gstr2Imps buildImps(Row r, Gstr2Filing f) {
        String inv = s(r, 0);
        if (blank(inv)) return null;
        return Gstr2Imps.builder().filing(f).createdBy(f.getCreatedBy())
                .invoiceNumberOfRegRecipient(inv).invoiceDate(d(r, 1)).invoiceValue(n(r, 2))
                .placeOfSupply(s(r, 3)).rate(n(r, 4)).taxableValue(z(n(r, 5)))
                .integratedTaxPaid(z(n(r, 6))).cessPaid(z(n(r, 7))).eligibilityForItc(s(r, 8))
                .availedItcIntegratedTax(z(n(r, 9))).availedItcCess(z(n(r, 10))).build();
    }

    // impg: portCode, boeNumber, boeDate, boeValue, docType, gstinOfSez, rate, taxVal,
    //       igst, cess, eligibility, availedIgst, availedCess
    private Gstr2Impg buildImpg(Row r, Gstr2Filing f) {
        String boe = s(r, 1);
        if (blank(boe)) return null;
        return Gstr2Impg.builder().filing(f).createdBy(f.getCreatedBy())
                .portCode(s(r, 0)).billOfEntryNumber(boe).billOfEntryDate(d(r, 2))
                .billOfEntryValue(n(r, 3)).documentType(s(r, 4)).gstinOfSezSupplier(s(r, 5))
                .rate(n(r, 6)).taxableValue(z(n(r, 7))).integratedTaxPaid(z(n(r, 8)))
                .cessPaid(z(n(r, 9))).eligibilityForItc(s(r, 10))
                .availedItcIntegratedTax(z(n(r, 11))).availedItcCess(z(n(r, 12))).build();
    }

    // cdnr: gstin, noteNo, noteDate, invNo, invDate, preGst, docType, reason, supplyType,
    //       noteValue, rate, taxVal, igst, cgst, sgst, cess, eligibility,
    //       availedIgst, availedCgst, availedSgst, availedCess
    private Gstr2Cdnr buildCdnr(Row r, Gstr2Filing f) {
        String gstin = s(r, 0), note = s(r, 1);
        if (blank(gstin) || blank(note)) return null;
        return Gstr2Cdnr.builder().filing(f).createdBy(f.getCreatedBy())
                .gstinOfSupplier(gstin.toUpperCase()).noteRefundVoucherNumber(note)
                .noteRefundVoucherDate(d(r, 2)).invoiceAdvancePaymentVoucherNumber(s(r, 3))
                .invoiceAdvancePaymentVoucherDate(d(r, 4)).preGst(s(r, 5)).documentType(s(r, 6))
                .reasonForIssuingDocument(s(r, 7)).supplyType(s(r, 8))
                .noteRefundVoucherValue(z(n(r, 9))).rate(n(r, 10)).taxableValue(z(n(r, 11)))
                .integratedTaxPaid(z(n(r, 12))).centralTaxPaid(z(n(r, 13))).stateUtTaxPaid(z(n(r, 14)))
                .cessPaid(z(n(r, 15))).eligibilityForItc(s(r, 16))
                .availedItcIntegratedTax(z(n(r, 17))).availedItcCentralTax(z(n(r, 18)))
                .availedItcStateUtTax(z(n(r, 19))).availedItcCess(z(n(r, 20))).build();
    }

    // cdnur: noteNo, noteDate, invNo, invDate, preGst, docType, reason, supplyType, invType,
    //        noteValue, rate, taxVal, igst, cgst, sgst, cess, eligibility,
    //        availedIgst, availedCgst, availedSgst, availedCess
    private Gstr2Cdnur buildCdnur(Row r, Gstr2Filing f) {
        String note = s(r, 0);
        if (blank(note)) return null;
        return Gstr2Cdnur.builder().filing(f).createdBy(f.getCreatedBy())
                .noteVoucherNumber(note).noteVoucherDate(d(r, 1))
                .invoiceAdvancePaymentVoucherNumber(s(r, 2)).invoiceAdvancePaymentVoucherDate(d(r, 3))
                .preGst(s(r, 4)).documentType(s(r, 5)).reasonForIssuingDocument(s(r, 6))
                .supplyType(s(r, 7)).invoiceType(s(r, 8)).noteVoucherValue(z(n(r, 9)))
                .rate(n(r, 10)).taxableValue(z(n(r, 11))).integratedTaxPaid(z(n(r, 12)))
                .centralTaxPaid(z(n(r, 13))).stateUtTaxPaid(z(n(r, 14))).cessPaid(z(n(r, 15)))
                .eligibilityForItc(s(r, 16)).availedItcIntegratedTax(z(n(r, 17)))
                .availedItcCentralTax(z(n(r, 18))).availedItcStateUtTax(z(n(r, 19)))
                .availedItcCess(z(n(r, 20))).build();
    }

    // at: pos, supplyType, grossAdvancePaid, cessAmount
    private Gstr2At buildAt(Row r, Gstr2Filing f) {
        String pos = s(r, 0);
        if (blank(pos)) return null;
        return Gstr2At.builder().filing(f).createdBy(f.getCreatedBy())
                .placeOfSupply(pos).supplyType(s(r, 1)).grossAdvancePaid(z(n(r, 2)))
                .cessAmount(z(n(r, 3))).build();
    }

    // atadj: pos, supplyType, grossAdvanceAdjusted, cessAdjusted
    private Gstr2Atadj buildAtadj(Row r, Gstr2Filing f) {
        String pos = s(r, 0);
        if (blank(pos)) return null;
        return Gstr2Atadj.builder().filing(f).createdBy(f.getCreatedBy())
                .placeOfSupply(pos).supplyType(s(r, 1)).grossAdvanceAdjusted(z(n(r, 2)))
                .cessAdjusted(z(n(r, 3))).build();
    }

    // exemp: description, compositionTaxablePerson, nilRated, exempted, nonGst
    private Gstr2Exemp buildExemp(Row r, Gstr2Filing f) {
        String desc = s(r, 0);
        if (blank(desc)) return null;
        return Gstr2Exemp.builder().filing(f).createdBy(f.getCreatedBy())
                .description(desc).compositionTaxablePerson(z(n(r, 1)))
                .nilRatedSupplies(z(n(r, 2))).exemptedSupplies(z(n(r, 3)))
                .nonGstSupplies(z(n(r, 4))).build();
    }

    // itcr: description, toBeAddedOrReduced, igst, cgst, sgst, cess
    private Gstr2Itcr buildItcr(Row r, Gstr2Filing f) {
        String desc = s(r, 0);
        if (blank(desc)) return null;
        return Gstr2Itcr.builder().filing(f).createdBy(f.getCreatedBy())
                .descriptionForReversalOfItc(desc).toBeAddedOrReduced(s(r, 1))
                .itcIntegratedTaxAmount(z(n(r, 2))).itcCentralTaxAmount(z(n(r, 3)))
                .itcStateUtTaxAmount(z(n(r, 4))).itcCessAmount(z(n(r, 5))).build();
    }

    // hsnsum: hsn, description, uqc, qty, totalValue, taxVal, igst, cgst, sgst, cess
    // Note: the workbook sometimes leaves the HSN cell blank on a follow-on data row
    // (merged-cell style export), so a row is kept as long as EITHER hsn or
    // description is present.
    private Gstr2HsnSum buildHsnSum(Row r, Gstr2Filing f) {
        String hsn = s(r, 0), desc = s(r, 1);
        if (blank(hsn) && blank(desc)) return null;
        return Gstr2HsnSum.builder().filing(f).createdBy(f.getCreatedBy())
                .hsn(hsn).description(desc).uqc(s(r, 2)).totalQuantity(n(r, 3))
                .totalValue(n(r, 4)).taxableValue(z(n(r, 5))).integratedTaxAmount(z(n(r, 6)))
                .centralTaxAmount(z(n(r, 7))).stateUtTaxAmount(z(n(r, 8))).cessAmount(z(n(r, 9)))
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    //  CELL HELPERS
    // ─────────────────────────────────────────────────────────────

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
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
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
    private boolean blank(String s)   { return s == null || s.isBlank(); }

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

    // ─────────────────────────────────────────────────────────────
    //  RESULT CONTAINER
    // ─────────────────────────────────────────────────────────────
    public static class ParseResult {
        public List<Gstr2B2b>    b2b    = new ArrayList<>();
        public List<Gstr2B2bur>  b2bur  = new ArrayList<>();
        public List<Gstr2Imps>   imps   = new ArrayList<>();
        public List<Gstr2Impg>   impg   = new ArrayList<>();
        public List<Gstr2Cdnr>   cdnr   = new ArrayList<>();
        public List<Gstr2Cdnur>  cdnur  = new ArrayList<>();
        public List<Gstr2At>     at     = new ArrayList<>();
        public List<Gstr2Atadj>  atadj  = new ArrayList<>();
        public List<Gstr2Exemp>  exemp  = new ArrayList<>();
        public List<Gstr2Itcr>   itcr   = new ArrayList<>();
        public List<Gstr2HsnSum> hsnSum = new ArrayList<>();

        public int totalRows() {
            return b2b.size() + b2bur.size() + imps.size() + impg.size()
                    + cdnr.size() + cdnur.size() + at.size() + atadj.size()
                    + exemp.size() + itcr.size() + hsnSum.size();
        }
    }
}