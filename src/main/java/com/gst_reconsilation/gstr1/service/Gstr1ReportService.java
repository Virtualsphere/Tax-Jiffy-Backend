package com.gst_reconsilation.gstr1.service;

import com.gst_reconsilation.gstr1.dto.report.Gstr1ReportResponse;
import com.gst_reconsilation.gstr1.dto.report.Gstr1ReportResponse.*;
import com.gst_reconsilation.gstr1.entity.*;
import com.gst_reconsilation.gstr1.repository.Gstr1FilingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the "sales return" (GSTR-1) style report for a filing, reusing the per-sheet
 * getters already exposed by {@link Gstr1UploadService}.
 *
 * NOTE / ASSUMPTIONS (schema does not carry enough columns for a 100% accurate split
 * in a few places — flagged inline with TODO):
 *  - Table 4 / Table 6 are both sourced from Gstr1B2b, split by its `invoiceType` field
 *    ("Regular" -> table 4, contains "SEZ" -> 6B, contains "Deemed" -> 6C), matching the
 *    combined "b2b,sez,de" Excel sheet used by ExcelParserService.
 *  - IGST/CGST/SGST splits are computed from taxableValue * rate, using the invoice's
 *    place-of-supply state code vs. the company's home state code (first 2 digits of
 *    the company GSTIN) to decide inter-state vs intra-state.
 *  - Table 8 (nil/exempt/non-GST) cannot be split into 8A/8B/8C/8D (registered/
 *    unregistered x inter/intra) because Gstr1Exemp only stores a free-text description.
 *    Everything is currently bucketed into 8A - wire real classification once the schema
 *    carries it.
 *  - basicData turnover / legal name / trade name are not present on any entity shown;
 *    only GSTIN is populated for real, the rest are placeholders.
 */
@Service
@RequiredArgsConstructor
public class Gstr1ReportService {

    private final Gstr1UploadService uploadService;
    private final Gstr1FilingRepository filingRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public Gstr1ReportResponse buildReport(Integer filingId) {
        Gstr1Filing filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Filing not found: " + filingId));

        String companyGstin = filing.getCompanyGST() != null ? filing.getCompanyGST().getGstNumber() : null;
        String homeState = stateCode(companyGstin);

        return Gstr1ReportResponse.builder()
                .basicData(buildBasicData(filing, companyGstin))
                .outwardData(buildOutwardData(filingId, homeState))
                .amendmentsData(buildAmendmentsData(filingId, homeState))
                .advancedData(buildAdvancedData(filingId, homeState))
                .othersData(buildOthersData(filingId))
                .build();
    }

    // ── Basic ───────────────────────────────────────────────────
    private List<BasicDataItem> buildBasicData(Gstr1Filing filing, String gstin) {
        List<BasicDataItem> items = new ArrayList<>();
        items.add(BasicDataItem.builder().sr("1").label("GSTIN")
                .sub("Goods and Services Tax Identification Number")
                .value(gstin != null ? gstin : "N/A").highlight(false).build());
        // TODO: legal name / trade name / turnover are not present on CompanyGST as
        // shown in the provided entities — wire these once the company profile
        // fields are available.
        items.add(BasicDataItem.builder().sr("2(a)").label("Legal Name")
                .sub("As per PAN database").value("N/A").highlight(false).build());
        items.add(BasicDataItem.builder().sr("2(b)").label("Trade Name")
                .sub("If different from legal name").value("N/A").highlight(false).build());
        items.add(BasicDataItem.builder().sr("3(a)").label("Aggregate Turnover (Preceding FY)")
                .sub("Turnover for financial year " + filing.getFinancialYear())
                .value("N/A").highlight(true).build());
        return items;
    }

    // ── Outward: Table 4,5,6,7,8 ────────────────────────────────
    private OutwardData buildOutwardData(Integer filingId, String homeState) {
        return OutwardData.builder()
                .table4(buildTable4(filingId, homeState))
                .table5(buildTable5(filingId, homeState))
                .table6(buildTable6(filingId))
                .table7(buildTable7(filingId, homeState))
                .table8(buildTable8(filingId))
                .build();
    }

    private Table4 buildTable4(Integer filingId, String homeState) {
        List<Gstr1B2b> regular = uploadService.getB2b(filingId).stream()
                .filter(b -> isRegular(b.getInvoiceType()))
                .collect(Collectors.toList());

        List<B2bRow> a = new ArrayList<>(), b = new ArrayList<>(), c = new ArrayList<>();
        String ecomGstin = null;
        BigDecimal invTot = BigDecimal.ZERO, txTot = BigDecimal.ZERO,
                igstTot = BigDecimal.ZERO, cgstTot = BigDecimal.ZERO,
                sgstTot = BigDecimal.ZERO, cessTot = BigDecimal.ZERO;

        for (Gstr1B2b r : regular) {
            String[] split = splitTax(r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState);
            B2bRow row = B2bRow.builder()
                    .gstin(r.getGstinOfRecipient()).invoiceNo(r.getInvoiceNumber())
                    .invoiceDate(dateStr(r.getInvoiceDate())).invoiceValue(money(r.getInvoiceValue()))
                    .taxableValue(money(r.getTaxableValue())).igst(split[0]).cgst(split[1]).sgst(split[2])
                    .cess(money(r.getCessAmount())).pos(r.getPlaceOfSupply()).build();

            boolean ecom = notBlank(r.getEcommerceGstin());
            boolean reverseCharge = "Y".equalsIgnoreCase(r.getReverseCharge());
            if (ecom) { c.add(row); if (ecomGstin == null) ecomGstin = r.getEcommerceGstin(); }
            else if (reverseCharge) b.add(row);
            else a.add(row);

            invTot = invTot.add(nz(r.getInvoiceValue()));
            txTot = txTot.add(nz(r.getTaxableValue()));
            igstTot = igstTot.add(new BigDecimal(split[0]));
            cgstTot = cgstTot.add(new BigDecimal(split[1]));
            sgstTot = sgstTot.add(new BigDecimal(split[2]));
            cessTot = cessTot.add(nz(r.getCessAmount()));
        }

        return Table4.builder().section4A(a).section4B(b)
                .section4C_ecommerceGstin(ecomGstin).section4C(c)
                .total(Totals.builder().invoiceValue(money(invTot)).taxableValue(money(txTot))
                        .igst(money(igstTot)).cgst(money(cgstTot)).sgst(money(sgstTot))
                        .cess(money(cessTot)).build())
                .build();
    }

    private Table5 buildTable5(Integer filingId, String homeState) {
        List<Gstr1B2cl> rows = uploadService.getB2cl(filingId);
        List<B2clRow> a = new ArrayList<>(), b = new ArrayList<>();
        String ecomGstin = null;
        BigDecimal invTot = BigDecimal.ZERO, txTot = BigDecimal.ZERO, igstTot = BigDecimal.ZERO;

        for (Gstr1B2cl r : rows) {
            String[] split = splitTax(r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState);
            B2clRow row = B2clRow.builder()
                    .gstin(r.getEcommerceGstin()).pos(r.getPlaceOfSupply())
                    .invoiceNo(r.getInvoiceNumber()).invoiceDate(dateStr(r.getInvoiceDate()))
                    .invoiceValue(money(r.getInvoiceValue())).rate(money(r.getRate()))
                    .taxableValue(money(r.getTaxableValue())).igst(split[0]).build();

            if (notBlank(r.getEcommerceGstin())) { b.add(row); if (ecomGstin == null) ecomGstin = r.getEcommerceGstin(); }
            else a.add(row);

            invTot = invTot.add(nz(r.getInvoiceValue()));
            txTot = txTot.add(nz(r.getTaxableValue()));
            igstTot = igstTot.add(new BigDecimal(split[0]));
        }

        return Table5.builder().section5A(a).section5B_ecommerceGstin(ecomGstin).section5B(b)
                .total(Totals.builder().invoiceValue(money(invTot)).taxableValue(money(txTot))
                        .igst(money(igstTot)).build())
                .build();
    }

    private Table6 buildTable6(Integer filingId) {
        List<ExportRow> exports = uploadService.getExp(filingId).stream().map(r ->
                ExportRow.builder().gstin(null).invoiceNo(r.getInvoiceNumber())
                        .invoiceDate(dateStr(r.getInvoiceDate())).invoiceValue(money(r.getInvoiceValue()))
                        .sbNo(r.getShippingBillNumber()).sbDate(dateStr(r.getShippingBillDate()))
                        .rate(money(r.getRate())).taxableValue(money(r.getTaxableValue()))
                        .amt(money(taxAmt(r.getTaxableValue(), r.getRate())))
                        .build()
        ).collect(Collectors.toList());

        List<Gstr1B2b> b2b = uploadService.getB2b(filingId);
        List<ExportRow> sez = b2b.stream()
                .filter(r -> containsIgnoreCase(r.getInvoiceType(), "SEZ"))
                .map(this::toExportRow).collect(Collectors.toList());
        List<ExportRow> deemed = b2b.stream()
                .filter(r -> containsIgnoreCase(r.getInvoiceType(), "Deemed"))
                .map(this::toExportRow).collect(Collectors.toList());

        return Table6.builder().section6A(exports).section6B(sez).section6C(deemed).build();
    }

    private ExportRow toExportRow(Gstr1B2b r) {
        return ExportRow.builder().gstin(r.getGstinOfRecipient()).invoiceNo(r.getInvoiceNumber())
                .invoiceDate(dateStr(r.getInvoiceDate())).invoiceValue(money(r.getInvoiceValue()))
                .sbNo("").sbDate("").rate(money(r.getRate())).taxableValue(money(r.getTaxableValue()))
                .amt(money(taxAmt(r.getTaxableValue(), r.getRate())))
                .build();
    }

    private Table7 buildTable7(Integer filingId, String homeState) {
        List<Gstr1B2cs> rows = uploadService.getB2cs(filingId);
        List<B2csRow> a1 = new ArrayList<>(), a2 = new ArrayList<>(), b1 = new ArrayList<>(), b2 = new ArrayList<>();
        String ecomA2 = null, ecomB2 = null, b1Pos = null;

        for (Gstr1B2cs r : rows) {
            String[] split = splitTax(r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState);
            B2csRow row = B2csRow.builder().rate(money(r.getRate())).stateName(r.getPlaceOfSupply())
                    .taxableValue(money(r.getTaxableValue()))
                    .integrated(split[0]).central(split[1]).state(split[2]).build();

            boolean intra = homeState != null && homeState.equals(stateCode(r.getPlaceOfSupply()));
            boolean ecom = notBlank(r.getEcommerceGstin());

            if (intra) {
                if (ecom) { a2.add(row); if (ecomA2 == null) ecomA2 = r.getEcommerceGstin(); }
                else a1.add(row);
            } else {
                if (ecom) { b2.add(row); if (ecomB2 == null) ecomB2 = r.getEcommerceGstin(); }
                else { b1.add(row); if (b1Pos == null) b1Pos = r.getPlaceOfSupply(); }
            }
        }

        return Table7.builder().section7A1(a1).section7A2_ecommerceGstin(ecomA2).section7A2(a2)
                .section7B1_pos(b1Pos).section7B1(b1)
                .section7B2_ecommerceGstin(ecomB2).section7B2(b2).build();
    }

    private Table8 buildTable8(Integer filingId) {
        // TODO: Gstr1Exemp has no registered/unregistered or inter/intra-state flag,
        // so everything is bucketed under 8A for now.
        List<Gstr1Exemp> rows = uploadService.getExemp(filingId);
        BigDecimal nil = BigDecimal.ZERO, exempt = BigDecimal.ZERO, nonGst = BigDecimal.ZERO;
        for (Gstr1Exemp r : rows) {
            nil = nil.add(nz(r.getNilRatedSupplies()));
            exempt = exempt.add(nz(r.getExemptedSupplies()));
            nonGst = nonGst.add(nz(r.getNonGstSupplies()));
        }
        ExemptRow a = ExemptRow.builder().label("Inter-State supplies to registered persons")
                .nilRated(money(nil)).exempted(money(exempt)).nonGst(money(nonGst)).build();
        ExemptRow empty = ExemptRow.builder().label("").nilRated("0.00").exempted("0.00").nonGst("0.00").build();
        ExemptRow total = ExemptRow.builder().label("TOTAL")
                .nilRated(money(nil)).exempted(money(exempt)).nonGst(money(nonGst)).build();

        return Table8.builder().section8A(a)
                .section8B(ExemptRow.builder().label("Intra-State supplies to registered persons")
                        .nilRated("0.00").exempted("0.00").nonGst("0.00").build())
                .section8C(ExemptRow.builder().label("Inter-State supplies to unregistered persons")
                        .nilRated("0.00").exempted("0.00").nonGst("0.00").build())
                .section8D(ExemptRow.builder().label("Intra-State supplies to unregistered persons")
                        .nilRated("0.00").exempted("0.00").nonGst("0.00").build())
                .total(total).build();
    }

    // ── Amendments: Table 9,10 ──────────────────────────────────
    private AmendmentsData buildAmendmentsData(Integer filingId, String homeState) {
        return AmendmentsData.builder()
                .table9(buildTable9(filingId, homeState))
                .table10(buildTable10(filingId, homeState))
                .build();
    }

    private Table9 buildTable9(Integer filingId, String homeState) {
        List<AmendRow> a = uploadService.getB2ba(filingId).stream().map(r -> {
            String[] split = splitTax(r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState);
            return AmendRow.builder()
                    .originalGstin(r.getGstinOfRecipient()).originalInvNo(r.getOriginalInvoiceNumber())
                    .originalInvDate(dateStr(r.getOriginalInvoiceDate()))
                    .revisedGstin(r.getGstinOfRecipient()).revisedInvNo(r.getRevisedInvoiceNumber())
                    .revisedInvDate(dateStr(r.getRevisedInvoiceDate()))
                    .sbNo("").sbDate("").value(money(r.getInvoiceValue())).rate(money(r.getRate()))
                    .taxableValue(money(r.getTaxableValue())).integratedTax(split[0]).build();
        }).collect(Collectors.toList());

        // Gstr1Cdnr has no original-invoice reference column (post-2020 GSTR-1 format
        // does not require one), so original* fields are left blank here.
        List<AmendRow> b = uploadService.getCdnr(filingId).stream().map(r -> {
            String[] split = splitTax(r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState);
            return AmendRow.builder()
                    .originalGstin(r.getGstinOfRecipient()).originalInvNo("").originalInvDate("")
                    .revisedGstin(r.getGstinOfRecipient()).revisedInvNo(r.getNoteNumber())
                    .revisedInvDate(dateStr(r.getNoteDate()))
                    .sbNo("").sbDate("").value(money(r.getNoteValue())).rate(money(r.getRate()))
                    .taxableValue(money(r.getTaxableValue())).integratedTax(split[0]).build();
        }).collect(Collectors.toList());

        List<AmendRow> c = uploadService.getCdnra(filingId).stream().map(r -> {
            String[] split = splitTax(r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState);
            return AmendRow.builder()
                    .originalGstin(r.getGstinOfRecipient()).originalInvNo(r.getOriginalNoteNumber())
                    .originalInvDate(dateStr(r.getOriginalNoteDate()))
                    .revisedGstin(r.getGstinOfRecipient()).revisedInvNo(r.getRevisedNoteNumber())
                    .revisedInvDate(dateStr(r.getRevisedNoteDate()))
                    .sbNo("").sbDate("").value(money(r.getNoteValue())).rate(money(r.getRate()))
                    .taxableValue(money(r.getTaxableValue())).integratedTax(split[0]).build();
        }).collect(Collectors.toList());

        return Table9.builder().section9A(a).section9B(b).section9C(c).build();
    }

    private Table10 buildTable10(Integer filingId, String homeState) {
        List<Gstr1B2csa> rows = uploadService.getB2csa(filingId);
        List<B2csRow> a = new ArrayList<>(), a1 = new ArrayList<>(), b = new ArrayList<>(), b1 = new ArrayList<>();
        String ecomA1 = null, ecomB1 = null, bPos = null;

        for (Gstr1B2csa r : rows) {
            String[] split = splitTax(r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState);
            B2csRow row = B2csRow.builder().rate(money(r.getRate())).stateName(r.getPlaceOfSupply())
                    .taxableValue(money(r.getTaxableValue()))
                    .integrated(split[0]).central(split[1]).state(split[2]).build();

            boolean intra = homeState != null && homeState.equals(stateCode(r.getPlaceOfSupply()));
            boolean ecom = notBlank(r.getEcommerceGstin());

            if (intra) {
                if (ecom) { a1.add(row); if (ecomA1 == null) ecomA1 = r.getEcommerceGstin(); }
                else a.add(row);
            } else {
                if (ecom) { b1.add(row); if (ecomB1 == null) ecomB1 = r.getEcommerceGstin(); }
                else { b.add(row); if (bPos == null) bPos = r.getPlaceOfSupply(); }
            }
        }

        return Table10.builder().section10A(a).section10A1_ecommerceGstin(ecomA1).section10A1(a1)
                .section10B_pos(bPos).section10B(b)
                .section10B1_ecommerceGstin(ecomB1).section10B1(b1).build();
    }

    // ── Advances: Table 11 ───────────────────────────────────────
    private AdvancedData buildAdvancedData(Integer filingId, String homeState) {
        List<AdvanceRow> a1 = new ArrayList<>(), a2 = new ArrayList<>(), b1 = new ArrayList<>(), b2 = new ArrayList<>();

        for (Gstr1At r : uploadService.getAt(filingId)) {
            AdvanceRow row = toAdvanceRow(r.getRate(), r.getGrossAdvanceReceived(), r.getPlaceOfSupply(),
                    r.getCessAmount(), homeState);
            if (isIntra(r.getPlaceOfSupply(), homeState)) a1.add(row); else a2.add(row);
        }
        for (Gstr1Atadj r : uploadService.getAtadj(filingId)) {
            AdvanceRow row = toAdvanceRow(r.getRate(), r.getGrossAdvanceAdjusted(), r.getPlaceOfSupply(),
                    r.getCessAmount(), homeState);
            if (isIntra(r.getPlaceOfSupply(), homeState)) b1.add(row); else b2.add(row);
        }

        List<AdvanceAmendRow> amendments = new ArrayList<>();
        for (Gstr1Ata r : uploadService.getAta(filingId)) {
            boolean intra = isIntra(r.getOriginalPlaceOfSupply(), homeState);
            amendments.add(AdvanceAmendRow.builder().month(r.getOriginalMonth())
                    .amendmentRelatingTo("11A")
                    .val11A1(intra ? money(r.getGrossAdvanceReceived()) : "0.00")
                    .val11A2(!intra ? money(r.getGrossAdvanceReceived()) : "0.00")
                    .val11B1("0.00").val11B2("0.00").build());
        }
        for (Gstr1Atadja r : uploadService.getAtadja(filingId)) {
            boolean intra = isIntra(r.getOriginalPlaceOfSupply(), homeState);
            amendments.add(AdvanceAmendRow.builder().month(r.getOriginalMonth())
                    .amendmentRelatingTo("11B")
                    .val11A1("0.00").val11A2("0.00")
                    .val11B1(intra ? money(r.getGrossAdvanceAdjusted()) : "0.00")
                    .val11B2(!intra ? money(r.getGrossAdvanceAdjusted()) : "0.00").build());
        }

        Table11 t11 = Table11.builder().section11A1(a1).section11A2(a2)
                .section11B1(b1).section11B2(b2).amendments(amendments).build();
        return AdvancedData.builder().table11(t11).build();
    }

    private AdvanceRow toAdvanceRow(BigDecimal rate, BigDecimal grossAdvance, String pos,
                                    BigDecimal cess, String homeState) {
        String[] split = splitTax(grossAdvance, rate, pos, homeState);
        return AdvanceRow.builder().rate(money(rate)).grossAdvance(money(grossAdvance)).pos(pos)
                .integrated(split[0]).central(split[1]).state(split[2]).cess(money(cess)).build();
    }

    // ── Others: Table 12,13 ──────────────────────────────────────
    private OthersData buildOthersData(Integer filingId) {
        List<HsnRow> records = new ArrayList<>();
        BigDecimal qty = BigDecimal.ZERO, val = BigDecimal.ZERO, tax = BigDecimal.ZERO,
                igst = BigDecimal.ZERO, cgst = BigDecimal.ZERO, sgst = BigDecimal.ZERO, cess = BigDecimal.ZERO;

        for (Gstr1HsnB2b r : uploadService.getHsnB2b(filingId)) {
            records.add(HsnRow.builder().hsn(r.getHsn()).description(r.getDescription()).uqc(r.getUqc())
                    .totalQuantity(qtyStr(r.getTotalQuantity())).totalValue(money(r.getTotalValue()))
                    .taxableValue(money(r.getTaxableValue())).integratedTax(money(r.getIntegratedTaxAmount()))
                    .centralTax(money(r.getCentralTaxAmount())).stateTax(money(r.getStateUtTaxAmount()))
                    .cess(money(r.getCessAmount())).build());
            qty = qty.add(nz(r.getTotalQuantity())); val = val.add(nz(r.getTotalValue()));
            tax = tax.add(nz(r.getTaxableValue())); igst = igst.add(nz(r.getIntegratedTaxAmount()));
            cgst = cgst.add(nz(r.getCentralTaxAmount())); sgst = sgst.add(nz(r.getStateUtTaxAmount()));
            cess = cess.add(nz(r.getCessAmount()));
        }
        for (Gstr1HsnB2c r : uploadService.getHsnB2c(filingId)) {
            records.add(HsnRow.builder().hsn(r.getHsn()).description(r.getDescription()).uqc(r.getUqc())
                    .totalQuantity(qtyStr(r.getTotalQuantity())).totalValue(money(r.getTotalValue()))
                    .taxableValue(money(r.getTaxableValue())).integratedTax(money(r.getIntegratedTaxAmount()))
                    .centralTax(money(r.getCentralTaxAmount())).stateTax(money(r.getStateUtTaxAmount()))
                    .cess(money(r.getCessAmount())).build());
            qty = qty.add(nz(r.getTotalQuantity())); val = val.add(nz(r.getTotalValue()));
            tax = tax.add(nz(r.getTaxableValue())); igst = igst.add(nz(r.getIntegratedTaxAmount()));
            cgst = cgst.add(nz(r.getCentralTaxAmount())); sgst = sgst.add(nz(r.getStateUtTaxAmount()));
            cess = cess.add(nz(r.getCessAmount()));
        }

        Table12 table12 = Table12.builder().records(records)
                .total(HsnTotal.builder().totalQuantity(qtyStr(qty)).totalValue(money(val))
                        .taxableValue(money(tax)).integratedTax(money(igst)).centralTax(money(cgst))
                        .stateTax(money(sgst)).cess(money(cess)).build())
                .build();

        List<DocRow> docs = uploadService.getDocs(filingId).stream().map(d -> {
            int total = d.getTotalNumber() != null ? d.getTotalNumber() : 0;
            int cancelledN = d.getCancelled() != null ? d.getCancelled() : 0;
            return DocRow.builder().natureOfDocument(d.getNatureOfDocument())
                    .from(d.getSrNoFrom()).to(d.getSrNoTo())
                    .totalNumber(String.valueOf(total)).cancelled(String.valueOf(cancelledN))
                    .netIssued(String.valueOf(total - cancelledN)).build();
        }).collect(Collectors.toList());

        Table13 table13 = Table13.builder().records(docs).build();

        return OthersData.builder().table12(table12).table13(table13).build();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private boolean isRegular(String invoiceType) {
        return invoiceType == null || invoiceType.trim().isEmpty()
                || invoiceType.trim().equalsIgnoreCase("Regular");
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private boolean isIntra(String pos, String homeState) {
        return homeState != null && homeState.equals(stateCode(pos));
    }

    /** First 2 characters of a GSTIN or a "NN-StateName" place-of-supply string. */
    private String stateCode(String posOrGstin) {
        if (posOrGstin == null || posOrGstin.length() < 2) return null;
        return posOrGstin.substring(0, 2);
    }

    private BigDecimal taxAmt(BigDecimal taxableValue, BigDecimal rate) {
        BigDecimal tv = nz(taxableValue), rt = nz(rate);
        return tv.multiply(rt).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** Returns {igst, cgst, sgst} as plain decimal strings, split by home-state comparison. */
    private String[] splitTax(BigDecimal taxableValue, BigDecimal rate, String pos, String homeState) {
        BigDecimal total = taxAmt(taxableValue, rate);
        if (isIntra(pos, homeState)) {
            BigDecimal half = total.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            return new String[]{ "0.00", money(half), money(half) };
        }
        return new String[]{ money(total), "0.00", "0.00" };
    }

    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private String money(BigDecimal v) {
        return nz(v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String qtyStr(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private String dateStr(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }
}