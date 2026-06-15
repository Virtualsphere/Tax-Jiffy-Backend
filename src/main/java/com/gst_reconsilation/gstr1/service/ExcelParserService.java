package com.gst_reconsilation.gstr1.service;

import com.gst_reconsilation.gstr1.entity.*;
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
 * Parses the official GSTR-1 Excel workbook (V2.2).
 * Rows 0-3 are header/summary rows; actual data starts at row index 4.
 */
@Service
public class ExcelParserService {

    private static final int DATA_START = 4;

    private static final String SH_B2B       = "b2b,sez,de";
    private static final String SH_B2BA      = "b2ba";
    private static final String SH_B2CL      = "b2cl";
    private static final String SH_B2CLA     = "b2cla";
    private static final String SH_B2CS      = "b2cs";
    private static final String SH_B2CSA     = "b2csa";
    private static final String SH_CDNR      = "cdnr";
    private static final String SH_CDNRA     = "cdnra";
    private static final String SH_CDNUR     = "cdnur";
    private static final String SH_CDNURA    = "cdnura";
    private static final String SH_EXP       = "exp";
    private static final String SH_EXPA      = "expa";
    private static final String SH_AT        = "at";
    private static final String SH_ATA       = "ata";
    private static final String SH_ATADJ     = "atadj";
    private static final String SH_ATADJA    = "atadja";
    private static final String SH_EXEMP     = "exemp";
    private static final String SH_HSN_B2B   = "hsn(b2b)";
    private static final String SH_HSN_B2C   = "hsn(b2c)";
    private static final String SH_DOCS      = "docs";
    private static final String SH_ECO       = "eco";
    private static final String SH_ECOA      = "ecoa";
    private static final String SH_ECOB2B    = "ecob2b";
    private static final String SH_ECOURP2B  = "ecourp2b";
    private static final String SH_ECOB2C    = "ecob2c";
    private static final String SH_ECOURP2C  = "ecourp2c";
    private static final String SH_ECOAB2B   = "ecoab2b";
    private static final String SH_ECOAB2C   = "ecoab2c";
    private static final String SH_ECOAURP2B = "ecoaurp2b";
    private static final String SH_ECOAURP2C = "ecoaurp2c";

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────
    public ParseResult parse(InputStream is, Gstr1Filing filing) throws Exception {
        ParseResult r = new ParseResult();
        try (Workbook wb = new XSSFWorkbook(is)) {
            r.b2b       = parseSheet(wb, SH_B2B,       row -> buildB2b(row, filing));
            r.b2ba      = parseSheet(wb, SH_B2BA,      row -> buildB2ba(row, filing));
            r.b2cl      = parseSheet(wb, SH_B2CL,      row -> buildB2cl(row, filing));
            r.b2cla     = parseSheet(wb, SH_B2CLA,     row -> buildB2cla(row, filing));
            r.b2cs      = parseSheet(wb, SH_B2CS,      row -> buildB2cs(row, filing));
            r.b2csa     = parseSheet(wb, SH_B2CSA,     row -> buildB2csa(row, filing));
            r.cdnr      = parseSheet(wb, SH_CDNR,      row -> buildCdnr(row, filing));
            r.cdnra     = parseSheet(wb, SH_CDNRA,     row -> buildCdnra(row, filing));
            r.cdnur     = parseSheet(wb, SH_CDNUR,     row -> buildCdnur(row, filing));
            r.cdnura    = parseSheet(wb, SH_CDNURA,    row -> buildCdnura(row, filing));
            r.exp       = parseSheet(wb, SH_EXP,       row -> buildExp(row, filing));
            r.expa      = parseSheet(wb, SH_EXPA,      row -> buildExpa(row, filing));
            r.at        = parseSheet(wb, SH_AT,        row -> buildAt(row, filing));
            r.ata       = parseSheet(wb, SH_ATA,       row -> buildAta(row, filing));
            r.atadj     = parseSheet(wb, SH_ATADJ,     row -> buildAtadj(row, filing));
            r.atadja    = parseSheet(wb, SH_ATADJA,    row -> buildAtadja(row, filing));
            r.exemp     = parseSheet(wb, SH_EXEMP,     row -> buildExemp(row, filing));
            r.hsnB2b    = parseSheet(wb, SH_HSN_B2B,  row -> buildHsnB2b(row, filing));
            r.hsnB2c    = parseSheet(wb, SH_HSN_B2C,  row -> buildHsnB2c(row, filing));
            r.docs      = parseSheet(wb, SH_DOCS,      row -> buildDocs(row, filing));
            r.eco       = parseSheet(wb, SH_ECO,       row -> buildEco(row, filing));
            r.ecoa      = parseSheet(wb, SH_ECOA,      row -> buildEcoa(row, filing));
            r.ecoB2b    = parseSheet(wb, SH_ECOB2B,   row -> buildEcoB2b(row, filing));
            r.ecoUrp2b  = parseSheet(wb, SH_ECOURP2B, row -> buildEcoUrp2b(row, filing));
            r.ecoB2c    = parseSheet(wb, SH_ECOB2C,   row -> buildEcoB2c(row, filing));
            r.ecoUrp2c  = parseSheet(wb, SH_ECOURP2C, row -> buildEcoUrp2c(row, filing));
            r.ecoAB2b   = parseSheet(wb, SH_ECOAB2B,  row -> buildEcoAB2b(row, filing));
            r.ecoAB2c   = parseSheet(wb, SH_ECOAB2C,  row -> buildEcoAB2c(row, filing));
            r.ecoAUrp2b = parseSheet(wb, SH_ECOAURP2B,row -> buildEcoAUrp2b(row, filing));
            r.ecoAUrp2c = parseSheet(wb, SH_ECOAURP2C,row -> buildEcoAUrp2c(row, filing));
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

    private Gstr1B2b buildB2b(Row r, Gstr1Filing f) {
        String gstin = s(r,0), inv = s(r,2);
        if (blank(gstin)||blank(inv)) return null;
        return Gstr1B2b.builder().filing(f).createdBy(f.getCreatedBy())
                .gstinOfRecipient(gstin.toUpperCase()).receiverName(s(r,1))
                .invoiceNumber(inv).invoiceDate(d(r,3)).invoiceValue(n(r,4))
                .placeOfSupply(s(r,5)).reverseCharge(s(r,6)).applicableTaxRatePct(n(r,7))
                .invoiceType(s(r,8)).ecommerceGstin(s(r,9)).rate(n(r,10))
                .taxableValue(z(n(r,11))).cessAmount(z(n(r,12))).build();
    }

    private Gstr1B2ba buildB2ba(Row r, Gstr1Filing f) {
        String gstin = s(r,0), orig = s(r,2);
        if (blank(gstin)||blank(orig)) return null;
        return Gstr1B2ba.builder().filing(f).createdBy(f.getCreatedBy())
                .gstinOfRecipient(gstin.toUpperCase()).receiverName(s(r,1))
                .originalInvoiceNumber(orig).originalInvoiceDate(d(r,3))
                .revisedInvoiceNumber(s(r,4)).revisedInvoiceDate(d(r,5))
                .invoiceValue(n(r,6)).placeOfSupply(s(r,7)).reverseCharge(s(r,8))
                .applicableTaxRatePct(n(r,9)).invoiceType(s(r,10)).ecommerceGstin(s(r,11))
                .rate(n(r,12)).taxableValue(z(n(r,13))).cessAmount(z(n(r,14))).build();
    }

    private Gstr1B2cl buildB2cl(Row r, Gstr1Filing f) {
        String inv = s(r,0);
        if (blank(inv)) return null;
        return Gstr1B2cl.builder().filing(f).createdBy(f.getCreatedBy())
                .invoiceNumber(inv).invoiceDate(d(r,1)).invoiceValue(n(r,2))
                .placeOfSupply(s(r,3)).applicableTaxRatePct(n(r,4)).rate(n(r,5))
                .taxableValue(z(n(r,6))).cessAmount(z(n(r,7))).ecommerceGstin(s(r,8)).build();
    }

    private Gstr1B2cla buildB2cla(Row r, Gstr1Filing f) {
        String orig = s(r,0);
        if (blank(orig)) return null;
        return Gstr1B2cla.builder().filing(f).createdBy(f.getCreatedBy())
                .originalInvoiceNumber(orig).originalInvoiceDate(d(r,1))
                .originalPlaceOfSupply(s(r,2)).revisedInvoiceNumber(s(r,3))
                .revisedInvoiceDate(d(r,4)).invoiceValue(n(r,5))
                .applicableTaxRatePct(n(r,6)).rate(n(r,7))
                .taxableValue(z(n(r,8))).cessAmount(z(n(r,9))).ecommerceGstin(s(r,10)).build();
    }

    private Gstr1B2cs buildB2cs(Row r, Gstr1Filing f) {
        String pos = s(r,1);
        if (blank(pos)) return null;
        return Gstr1B2cs.builder().filing(f).createdBy(f.getCreatedBy())
                .type(s(r,0)).placeOfSupply(pos).applicableTaxRatePct(n(r,2))
                .rate(n(r,3)).taxableValue(z(n(r,4))).cessAmount(z(n(r,5))).ecommerceGstin(s(r,6)).build();
    }

    private Gstr1B2csa buildB2csa(Row r, Gstr1Filing f) {
        String fy = s(r,0);
        if (blank(fy)) return null;
        return Gstr1B2csa.builder().filing(f).createdBy(f.getCreatedBy())
                .financialYear(fy).originalMonth(s(r,1)).placeOfSupply(s(r,2)).type(s(r,3))
                .applicableTaxRatePct(n(r,4)).rate(n(r,5)).taxableValue(z(n(r,6)))
                .cessAmount(z(n(r,7))).ecommerceGstin(s(r,8)).build();
    }

    private Gstr1Cdnr buildCdnr(Row r, Gstr1Filing f) {
        String gstin = s(r,0), note = s(r,2);
        if (blank(gstin)||blank(note)) return null;
        return Gstr1Cdnr.builder().filing(f).createdBy(f.getCreatedBy())
                .gstinOfRecipient(gstin.toUpperCase()).receiverName(s(r,1))
                .noteNumber(note).noteDate(d(r,3)).noteType(s(r,4))
                .placeOfSupply(s(r,5)).reverseCharge(s(r,6)).noteSupplyType(s(r,7))
                .noteValue(z(n(r,8))).applicableTaxRatePct(n(r,9)).rate(n(r,10))
                .taxableValue(z(n(r,11))).cessAmount(z(n(r,12))).build();
    }

    private Gstr1Cdnra buildCdnra(Row r, Gstr1Filing f) {
        String gstin = s(r,0), orig = s(r,2);
        if (blank(gstin)||blank(orig)) return null;
        return Gstr1Cdnra.builder().filing(f).createdBy(f.getCreatedBy())
                .gstinOfRecipient(gstin.toUpperCase()).receiverName(s(r,1))
                .originalNoteNumber(orig).originalNoteDate(d(r,3))
                .revisedNoteNumber(s(r,4)).revisedNoteDate(d(r,5))
                .noteType(s(r,6)).placeOfSupply(s(r,7)).reverseCharge(s(r,8))
                .noteSupplyType(s(r,9)).noteValue(z(n(r,10))).applicableTaxRatePct(n(r,11))
                .rate(n(r,12)).taxableValue(z(n(r,13))).cessAmount(z(n(r,14))).build();
    }

    private Gstr1Cdnur buildCdnur(Row r, Gstr1Filing f) {
        String note = s(r,1);
        if (blank(note)) return null;
        return Gstr1Cdnur.builder().filing(f).createdBy(f.getCreatedBy())
                .urType(s(r,0)).noteNumber(note).noteDate(d(r,2)).noteType(s(r,3))
                .placeOfSupply(s(r,4)).noteValue(z(n(r,5))).applicableTaxRatePct(n(r,6))
                .rate(n(r,7)).taxableValue(z(n(r,8))).cessAmount(z(n(r,9))).build();
    }

    private Gstr1Cdnura buildCdnura(Row r, Gstr1Filing f) {
        String orig = s(r,1);
        if (blank(orig)) return null;
        return Gstr1Cdnura.builder().filing(f).createdBy(f.getCreatedBy())
                .urType(s(r,0)).originalNoteNumber(orig).originalNoteDate(d(r,2))
                .revisedNoteNumber(s(r,3)).revisedNoteDate(d(r,4)).noteType(s(r,5))
                .placeOfSupply(s(r,6)).noteValue(z(n(r,7))).applicableTaxRatePct(n(r,8))
                .rate(n(r,9)).taxableValue(z(n(r,10))).cessAmount(z(n(r,11))).build();
    }

    private Gstr1Exp buildExp(Row r, Gstr1Filing f) {
        String inv = s(r,1);
        if (blank(inv)) return null;
        return Gstr1Exp.builder().filing(f).createdBy(f.getCreatedBy())
                .exportType(s(r,0)).invoiceNumber(inv).invoiceDate(d(r,2)).invoiceValue(n(r,3))
                .portCode(s(r,4)).shippingBillNumber(s(r,5)).shippingBillDate(d(r,6))
                .rate(n(r,7)).taxableValue(z(n(r,8))).cessAmount(z(n(r,9))).build();
    }

    private Gstr1Expa buildExpa(Row r, Gstr1Filing f) {
        String orig = s(r,1);
        if (blank(orig)) return null;
        return Gstr1Expa.builder().filing(f).createdBy(f.getCreatedBy())
                .exportType(s(r,0)).originalInvoiceNumber(orig).originalInvoiceDate(d(r,2))
                .revisedInvoiceNumber(s(r,3)).revisedInvoiceDate(d(r,4)).invoiceValue(n(r,5))
                .portCode(s(r,6)).shippingBillNumber(s(r,7)).shippingBillDate(d(r,8))
                .rate(n(r,9)).taxableValue(z(n(r,10))).cessAmount(z(n(r,11))).build();
    }

    private Gstr1At buildAt(Row r, Gstr1Filing f) {
        String pos = s(r,0);
        if (blank(pos)) return null;
        return Gstr1At.builder().filing(f).createdBy(f.getCreatedBy())
                .placeOfSupply(pos).applicableTaxRatePct(n(r,1)).rate(n(r,2))
                .grossAdvanceReceived(z(n(r,3))).cessAmount(z(n(r,4))).build();
    }

    private Gstr1Ata buildAta(Row r, Gstr1Filing f) {
        String fy = s(r,0);
        if (blank(fy)) return null;
        return Gstr1Ata.builder().filing(f).createdBy(f.getCreatedBy())
                .financialYear(fy).originalMonth(s(r,1)).originalPlaceOfSupply(s(r,2))
                .applicableTaxRatePct(n(r,3)).rate(n(r,4))
                .grossAdvanceReceived(z(n(r,5))).cessAmount(z(n(r,6))).build();
    }

    private Gstr1Atadj buildAtadj(Row r, Gstr1Filing f) {
        String pos = s(r,0);
        if (blank(pos)) return null;
        return Gstr1Atadj.builder().filing(f).createdBy(f.getCreatedBy())
                .placeOfSupply(pos).applicableTaxRatePct(n(r,1)).rate(n(r,2))
                .grossAdvanceAdjusted(z(n(r,3))).cessAmount(z(n(r,4))).build();
    }

    private Gstr1Atadja buildAtadja(Row r, Gstr1Filing f) {
        String fy = s(r,0);
        if (blank(fy)) return null;
        return Gstr1Atadja.builder().filing(f).createdBy(f.getCreatedBy())
                .financialYear(fy).originalMonth(s(r,1)).originalPlaceOfSupply(s(r,2))
                .applicableTaxRatePct(n(r,3)).rate(n(r,4))
                .grossAdvanceAdjusted(z(n(r,5))).cessAmount(z(n(r,6))).build();
    }

    private Gstr1Exemp buildExemp(Row r, Gstr1Filing f) {
        String desc = s(r,0);
        if (blank(desc)) return null;
        return Gstr1Exemp.builder().filing(f).createdBy(f.getCreatedBy())
                .description(desc).nilRatedSupplies(z(n(r,1)))
                .exemptedSupplies(z(n(r,2))).nonGstSupplies(z(n(r,3))).build();
    }

    private Gstr1HsnB2b buildHsnB2b(Row r, Gstr1Filing f) {
        String hsn = s(r,0);
        if (blank(hsn)) return null;
        return Gstr1HsnB2b.builder().filing(f).createdBy(f.getCreatedBy())
                .hsn(hsn).description(s(r,1)).uqc(s(r,2))
                .totalQuantity(n(r,3)).totalValue(n(r,4)).rate(n(r,5))
                .taxableValue(z(n(r,6))).integratedTaxAmount(z(n(r,7)))
                .centralTaxAmount(z(n(r,8))).stateUtTaxAmount(z(n(r,9))).cessAmount(z(n(r,10))).build();
    }

    private Gstr1HsnB2c buildHsnB2c(Row r, Gstr1Filing f) {
        String hsn = s(r,0);
        if (blank(hsn)) return null;
        return Gstr1HsnB2c.builder().filing(f).createdBy(f.getCreatedBy())
                .hsn(hsn).description(s(r,1)).uqc(s(r,2))
                .totalQuantity(n(r,3)).totalValue(n(r,4)).rate(n(r,5))
                .taxableValue(z(n(r,6))).integratedTaxAmount(z(n(r,7)))
                .centralTaxAmount(z(n(r,8))).stateUtTaxAmount(z(n(r,9))).cessAmount(z(n(r,10))).build();
    }

    private Gstr1Docs buildDocs(Row r, Gstr1Filing f) {
        String nature = s(r,0);
        if (blank(nature)) return null;
        BigDecimal tot = n(r,3), can = n(r,4);
        return Gstr1Docs.builder().filing(f).createdBy(f.getCreatedBy())
                .natureOfDocument(nature).srNoFrom(s(r,1)).srNoTo(s(r,2))
                .totalNumber(tot!=null?tot.intValue():0)
                .cancelled(can!=null?can.intValue():0).build();
    }

    private Gstr1Eco buildEco(Row r, Gstr1Filing f) {
        String gstin = s(r,1);
        if (blank(gstin)) return null;
        return Gstr1Eco.builder().filing(f).createdBy(f.getCreatedBy())
                .natureOfSupply(s(r,0)).ecommerceGstin(gstin.toUpperCase())
                .ecommerceOperatorName(s(r,2)).netValueOfSupplies(z(n(r,3)))
                .integratedTax(z(n(r,4))).centralTax(z(n(r,5))).stateUtTax(z(n(r,6))).cess(z(n(r,7))).build();
    }

    private Gstr1Ecoa buildEcoa(Row r, Gstr1Filing f) {
        String fy = s(r,1);
        if (blank(fy)) return null;
        return Gstr1Ecoa.builder().filing(f).createdBy(f.getCreatedBy())
                .natureOfSupply(s(r,0)).financialYear(fy).originalMonthQuarter(s(r,2))
                .originalEcommerceGstin(s(r,3)).revisedEcommerceGstin(s(r,4))
                .ecommerceOperatorName(s(r,5)).revisedNetValueOfSupplies(z(n(r,6)))
                .integratedTax(z(n(r,7))).centralTax(z(n(r,8))).stateUtTax(z(n(r,9))).cess(z(n(r,10))).build();
    }

    private Gstr1EcoB2b buildEcoB2b(Row r, Gstr1Filing f) {
        String sg = s(r,0), doc = s(r,4);
        if (blank(sg)||blank(doc)) return null;
        return Gstr1EcoB2b.builder().filing(f).createdBy(f.getCreatedBy())
                .supplierGstin(sg.toUpperCase()).supplierName(s(r,1))
                .recipientGstin(s(r,2)).recipientName(s(r,3))
                .documentNumber(doc).documentDate(d(r,5)).valueOfSuppliesMade(z(n(r,6)))
                .placeOfSupply(s(r,7)).documentType(s(r,8)).rate(n(r,9))
                .taxableValue(z(n(r,10))).cessAmount(z(n(r,11))).build();
    }

    private Gstr1EcoUrp2b buildEcoUrp2b(Row r, Gstr1Filing f) {
        String rg = s(r,0), doc = s(r,2);
        if (blank(rg)||blank(doc)) return null;
        return Gstr1EcoUrp2b.builder().filing(f).createdBy(f.getCreatedBy())
                .recipientGstin(rg.toUpperCase()).recipientName(s(r,1))
                .documentNumber(doc).documentDate(d(r,3)).valueOfSuppliesMade(z(n(r,4)))
                .placeOfSupply(s(r,5)).documentType(s(r,6)).rate(n(r,7))
                .taxableValue(z(n(r,8))).cessAmount(z(n(r,9))).build();
    }

    private Gstr1EcoB2c buildEcoB2c(Row r, Gstr1Filing f) {
        String sg = s(r,0);
        if (blank(sg)) return null;
        return Gstr1EcoB2c.builder().filing(f).createdBy(f.getCreatedBy())
                .supplierGstin(sg.toUpperCase()).supplierName(s(r,1))
                .placeOfSupply(s(r,2)).taxableValue(z(n(r,3))).rate(n(r,4)).cessAmount(z(n(r,5))).build();
    }

    private Gstr1EcoUrp2c buildEcoUrp2c(Row r, Gstr1Filing f) {
        String pos = s(r,0);
        if (blank(pos)) return null;
        return Gstr1EcoUrp2c.builder().filing(f).createdBy(f.getCreatedBy())
                .placeOfSupply(pos).taxableValue(z(n(r,1))).rate(n(r,2)).cessAmount(z(n(r,3))).build();
    }

    private Gstr1EcoAB2b buildEcoAB2b(Row r, Gstr1Filing f) {
        String sg = s(r,0), orig = s(r,4);
        if (blank(sg)||blank(orig)) return null;
        return Gstr1EcoAB2b.builder().filing(f).createdBy(f.getCreatedBy())
                .supplierGstin(sg.toUpperCase()).supplierName(s(r,1))
                .recipientGstin(s(r,2)).recipientName(s(r,3))
                .originalDocumentNumber(orig).originalDocumentDate(d(r,5))
                .revisedDocumentNumber(s(r,6)).revisedDocumentDate(d(r,7))
                .valueOfSuppliesMade(z(n(r,8))).placeOfSupply(s(r,9)).documentType(s(r,10))
                .rate(n(r,11)).taxableValue(z(n(r,12))).cessAmount(z(n(r,13))).build();
    }

    private Gstr1EcoAB2c buildEcoAB2c(Row r, Gstr1Filing f) {
        String fy = s(r,0);
        if (blank(fy)) return null;
        return Gstr1EcoAB2c.builder().filing(f).createdBy(f.getCreatedBy())
                .financialYear(fy).originalMonth(s(r,1)).supplierGstin(s(r,2))
                .supplierName(s(r,3)).placeOfSupply(s(r,4)).rate(n(r,5))
                .taxableValue(z(n(r,6))).cessAmount(z(n(r,7))).build();
    }

    private Gstr1EcoAUrp2b buildEcoAUrp2b(Row r, Gstr1Filing f) {
        String rg = s(r,0), orig = s(r,2);
        if (blank(rg)||blank(orig)) return null;
        return Gstr1EcoAUrp2b.builder().filing(f).createdBy(f.getCreatedBy())
                .recipientGstin(rg.toUpperCase()).recipientName(s(r,1))
                .originalDocumentNumber(orig).originalDocumentDate(d(r,3))
                .revisedDocumentNumber(s(r,4)).revisedDocumentDate(d(r,5))
                .valueOfSuppliesMade(z(n(r,6))).documentType(s(r,7)).placeOfSupply(s(r,8))
                .rate(n(r,9)).taxableValue(z(n(r,10))).cessAmount(z(n(r,11))).build();
    }

    private Gstr1EcoAUrp2c buildEcoAUrp2c(Row r, Gstr1Filing f) {
        String fy = s(r,0);
        if (blank(fy)) return null;
        return Gstr1EcoAUrp2c.builder().filing(f).createdBy(f.getCreatedBy())
                .financialYear(fy).originalMonth(s(r,1)).placeOfSupply(s(r,2))
                .rate(n(r,3)).taxableValue(z(n(r,4))).cessAmount(z(n(r,5))).build();
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
                yield v==Math.floor(v) ? String.valueOf((long)v) : String.valueOf(v); }
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
            for (DateTimeFormatter f : DATE_FMTS) {
                try { return LocalDate.parse(sv, f); } catch (Exception ignored) {}
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
        public List<Gstr1B2b>       b2b       = new ArrayList<>();
        public List<Gstr1B2ba>      b2ba      = new ArrayList<>();
        public List<Gstr1B2cl>      b2cl      = new ArrayList<>();
        public List<Gstr1B2cla>     b2cla     = new ArrayList<>();
        public List<Gstr1B2cs>      b2cs      = new ArrayList<>();
        public List<Gstr1B2csa>     b2csa     = new ArrayList<>();
        public List<Gstr1Cdnr>      cdnr      = new ArrayList<>();
        public List<Gstr1Cdnra>     cdnra     = new ArrayList<>();
        public List<Gstr1Cdnur>     cdnur     = new ArrayList<>();
        public List<Gstr1Cdnura>    cdnura    = new ArrayList<>();
        public List<Gstr1Exp>       exp       = new ArrayList<>();
        public List<Gstr1Expa>      expa      = new ArrayList<>();
        public List<Gstr1At>        at        = new ArrayList<>();
        public List<Gstr1Ata>       ata       = new ArrayList<>();
        public List<Gstr1Atadj>     atadj     = new ArrayList<>();
        public List<Gstr1Atadja>    atadja    = new ArrayList<>();
        public List<Gstr1Exemp>     exemp     = new ArrayList<>();
        public List<Gstr1HsnB2b>    hsnB2b    = new ArrayList<>();
        public List<Gstr1HsnB2c>    hsnB2c    = new ArrayList<>();
        public List<Gstr1Docs>      docs      = new ArrayList<>();
        public List<Gstr1Eco>       eco       = new ArrayList<>();
        public List<Gstr1Ecoa>      ecoa      = new ArrayList<>();
        public List<Gstr1EcoB2b>    ecoB2b    = new ArrayList<>();
        public List<Gstr1EcoUrp2b>  ecoUrp2b  = new ArrayList<>();
        public List<Gstr1EcoB2c>    ecoB2c    = new ArrayList<>();
        public List<Gstr1EcoUrp2c>  ecoUrp2c  = new ArrayList<>();
        public List<Gstr1EcoAB2b>   ecoAB2b   = new ArrayList<>();
        public List<Gstr1EcoAB2c>   ecoAB2c   = new ArrayList<>();
        public List<Gstr1EcoAUrp2b> ecoAUrp2b = new ArrayList<>();
        public List<Gstr1EcoAUrp2c> ecoAUrp2c = new ArrayList<>();

        public int totalRows() {
            return b2b.size()+b2ba.size()+b2cl.size()+b2cla.size()+b2cs.size()+b2csa.size()
                    +cdnr.size()+cdnra.size()+cdnur.size()+cdnura.size()
                    +exp.size()+expa.size()+at.size()+ata.size()+atadj.size()+atadja.size()
                    +exemp.size()+hsnB2b.size()+hsnB2c.size()+docs.size()
                    +eco.size()+ecoa.size()+ecoB2b.size()+ecoUrp2b.size()
                    +ecoB2c.size()+ecoUrp2c.size()+ecoAB2b.size()+ecoAB2c.size()
                    +ecoAUrp2b.size()+ecoAUrp2c.size();
        }
    }
}