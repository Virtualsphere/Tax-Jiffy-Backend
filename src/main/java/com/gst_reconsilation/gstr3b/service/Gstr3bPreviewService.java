package com.gst_reconsilation.gstr3b.service;

import com.gst_reconsilation.gstr1.entity.*;
import com.gst_reconsilation.gstr1.service.Gstr1UploadService;
import com.gst_reconsilation.gstr3b.dto.Gstr3bPreviewResponse;
import com.gst_reconsilation.gstr3b.dto.Gstr3bPreviewResponse.*;
import com.gst_reconsilation.gstr3b.entity.*;
import com.gst_reconsilation.gstr3b.repository.Gstr3bFilingRepository;
import com.gst_reconsilation.gstr3b.repository.Gstr3bItcSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds the GSTR-3B preview (tables 3.1, 3.2, 4, 5, 5.1, 6.1) for a
 * {@link Gstr3bFiling}, pulling from:
 *  - Gstr1UploadService (outward supplies already uploaded/synced under GSTR-1)
 *  - Gstr2UploadService (purchase register already uploaded under this module)
 *  - Gstr3bItcSummaryRepository (GSTR-2B itcsumm, synced via {@link Gstr3bTwoBSyncService})
 *
 * Wherever GSTR-2B hasn't been synced yet (or a category has no GSTR-2B
 * equivalent), the corresponding purchase-register "availed ITC" columns are
 * used as a fallback so the preview is still usable before a 2B sync. Every
 * such fallback / approximation is flagged with a TODO comment - these are the
 * spots most likely to need a tax-expert's sign-off on categorisation.
 *
 * Challan generation and the actual filing submission are NOT part of this
 * service (out of scope per current requirements).
 */
@Service
@RequiredArgsConstructor
public class Gstr3bPreviewService {

    private final Gstr3bFilingRepository filingRepository;
    private final Gstr3bItcSummaryRepository itcSummaryRepository;
    private final Gstr1UploadService gstr1Service;
    private final Gstr2UploadService gstr2Service;

    public Gstr3bPreviewResponse buildPreview(Integer gstr3bFilingId) {
        Gstr3bFiling filing = filingRepository.findById(gstr3bFilingId)
                .orElseThrow(() -> new RuntimeException("Gstr3bFiling not found: " + gstr3bFilingId));

        String homeState = stateCode(filing.getCompanyGST() != null ? filing.getCompanyGST().getGstNumber() : null);
        Integer gstr1Id = filing.getGstr1FilingId();
        Integer gstr2Id = filing.getGstr2FilingId();

        Table31 table31 = buildTable31(gstr1Id, gstr2Id, homeState);
        Table32 table32 = buildTable32(gstr1Id, homeState);
        Table4 table4 = buildTable4(gstr3bFilingId, gstr2Id);
        Table5 table5 = buildTable5(gstr2Id);
        Table51 table51 = buildTable51(filing);
        Table61 table61 = buildTable61(table31, table4, table51);

        Gstr3b body = Gstr3b.builder()
                .table_3_1_outward_and_reverse_charge_inward_supplies(table31)
                .table_3_2_interstate_supplies(table32)
                .table_4_eligible_itc(table4)
                .table_5_exempt_nil_nongst_inward_supplies(table5)
                .table_5_1_interest_and_late_fee(table51)
                .table_6_1_payment_of_tax(table61)
                .build();

        return Gstr3bPreviewResponse.builder().gstr3b(body).build();
    }

    // ── Table 3.1 ────────────────────────────────────────────────
    private Table31 buildTable31(Integer gstr1Id, Integer gstr2Id, String homeState) {
        BigDecimal[] a = gstr1Id == null ? zeros5() : sumOutwardRegular(gstr1Id, homeState);
        BigDecimal[] b = gstr1Id == null ? zeros5() : sumZeroRated(gstr1Id);
        BigDecimal[] c = gstr1Id == null ? zeros5() : sumNilExempt(gstr1Id);
        BigDecimal[] d = gstr2Id == null ? zeros5() : sumReverseChargeInward(gstr2Id, homeState);
        BigDecimal[] e = gstr1Id == null ? zeros5() : sumNonGstOutward(gstr1Id);

        List<Row31> rows = new ArrayList<>();
        rows.add(row31("a", "(a) Outward taxable supplies (other than zero rated, nil rated and exempted)", a, true));
        rows.add(row31("b", "(b) Outward taxable supplies (zero rated)", b, true));
        rows.add(row31("c", "(c) Other outward supplies (Nil rated, exempted)", c, false));
        rows.add(row31("d", "(d) Inward supplies (liable to reverse charge)", d, true));
        rows.add(row31("e", "(e) Non-GST outward supplies", e, false));

        BigDecimal[] total = new BigDecimal[5];
        for (int i = 0; i < 5; i++) {
            total[i] = nz(a[i]).add(nz(b[i])).add(nz(c[i])).add(nz(d[i])).add(nz(e[i]));
        }

        return Table31.builder()
                .title("3.1 Details of Outward Supplies and inward supplies liable to reverse charge")
                .headers(List.of("Nature of Supplies", "Total Taxable Value", "Integrated Tax", "Central Tax", "State/UT Tax", "Cess"))
                .rows(rows)
                .total(Total31.builder().taxable_value(rnd(total[0])).integrated_tax(rnd(total[1]))
                        .central_tax(rnd(total[2])).state_ut_tax(rnd(total[3])).cess(rnd(total[4])).build())
                .build();
    }

    private Row31 row31(String id, String label, BigDecimal[] v, boolean hasTax) {
        return Row31.builder().id(id).nature_of_supply(label).taxable_value(rnd(v[0]))
                .integrated_tax(hasTax ? rnd(v[1]) : null).central_tax(hasTax ? rnd(v[2]) : null)
                .state_ut_tax(hasTax ? rnd(v[3]) : null).cess(hasTax ? rnd(v[4]) : null).build();
    }

    /** {taxableValue, igst, cgst, sgst, cess} for regular (non-SEZ/Deemed/export) B2B + B2CL + B2CS. */
    private BigDecimal[] sumOutwardRegular(Integer gstr1Id, String homeState) {
        BigDecimal[] tot = zeros5();
        for (Gstr1B2b r : gstr1Service.getB2b(gstr1Id)) {
            if (!isRegularInvoiceType(r.getInvoiceType())) continue;
            add(tot, r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState, r.getCessAmount());
        }
        for (Gstr1B2cl r : gstr1Service.getB2cl(gstr1Id)) {
            add(tot, r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState, r.getCessAmount());
        }
        for (Gstr1B2cs r : gstr1Service.getB2cs(gstr1Id)) {
            add(tot, r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState, r.getCessAmount());
        }
        return tot;
    }

    /** {taxableValue, igst, 0, 0, cess} for exports + SEZ. */
    private BigDecimal[] sumZeroRated(Integer gstr1Id) {
        BigDecimal tv = BigDecimal.ZERO, igst = BigDecimal.ZERO, cess = BigDecimal.ZERO;
        for (Gstr1Exp r : gstr1Service.getExp(gstr1Id)) {
            tv = tv.add(nz(r.getTaxableValue()));
            igst = igst.add(taxAmt(r.getTaxableValue(), r.getRate()));
            cess = cess.add(nz(r.getCessAmount()));
        }
        for (Gstr1B2b r : gstr1Service.getB2b(gstr1Id)) {
            if (!containsIgnoreCase(r.getInvoiceType(), "SEZ")) continue;
            tv = tv.add(nz(r.getTaxableValue()));
            igst = igst.add(taxAmt(r.getTaxableValue(), r.getRate()));
            cess = cess.add(nz(r.getCessAmount()));
        }
        return new BigDecimal[]{tv, igst, BigDecimal.ZERO, BigDecimal.ZERO, cess};
    }

    /** {taxableValue, null...} for nil-rated + exempted outward supplies. */
    private BigDecimal[] sumNilExempt(Integer gstr1Id) {
        BigDecimal tv = BigDecimal.ZERO;
        for (Gstr1Exemp r : gstr1Service.getExemp(gstr1Id)) {
            tv = tv.add(nz(r.getNilRatedSupplies())).add(nz(r.getExemptedSupplies()));
        }
        return new BigDecimal[]{tv, null, null, null, null};
    }

    /** {taxableValue, null...} for non-GST outward supplies. */
    private BigDecimal[] sumNonGstOutward(Integer gstr1Id) {
        BigDecimal tv = BigDecimal.ZERO;
        for (Gstr1Exemp r : gstr1Service.getExemp(gstr1Id)) {
            tv = tv.add(nz(r.getNonGstSupplies()));
        }
        return new BigDecimal[]{tv, null, null, null, null};
    }

    /** {taxableValue, igst, cgst, sgst, cess} for inward supplies liable to reverse charge.
     *  TODO: only Gstr2B2b carries a reverseCharge column in the current schema.
     *  Gstr2B2bur, Gstr2Cdnr and Gstr2Cdnur have no such flag on the actual GSTR-2
     *  template, so RCM liability from those sheets isn't captured here - their
     *  ITC is instead counted entirely under "All other ITC" (see fallbackAllOtherItc). */
    private BigDecimal[] sumReverseChargeInward(Integer gstr2Id, String homeState) {
        BigDecimal[] tot = zeros5();
        for (Gstr2B2b r : gstr2Service.getB2b(gstr2Id)) {
            if (!"Y".equalsIgnoreCase(r.getReverseCharge())) continue;
            add(tot, r.getTaxableValue(), r.getRate(), r.getPlaceOfSupply(), homeState, r.getCessPaid());
        }
        return tot;
    }

    // ── Table 3.2 ────────────────────────────────────────────────
    /** TODO: Gstr1B2cs has no composition/UIN classification, so those two sections
     *  are always empty here - only the "Unregistered Persons" section is populated,
     *  grouped by place-of-supply state, for B2CS rows outside the home state. */
    private Table32 buildTable32(Integer gstr1Id, String homeState) {
        Map<String, BigDecimal[]> byState = new LinkedHashMap<>();
        if (gstr1Id != null) {
            for (Gstr1B2cs r : gstr1Service.getB2cs(gstr1Id)) {
                if (isIntra(r.getPlaceOfSupply(), homeState)) continue;
                BigDecimal[] agg = byState.computeIfAbsent(r.getPlaceOfSupply(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                agg[0] = agg[0].add(nz(r.getTaxableValue()));
                agg[1] = agg[1].add(taxAmt(r.getTaxableValue(), r.getRate()));
            }
        }
        List<Row32> urpRows = byState.entrySet().stream()
                .map(en -> Row32.builder().place_of_supply(en.getKey())
                        .taxable_value(rnd(en.getValue()[0])).integrated_tax(rnd(en.getValue()[1])).build())
                .collect(Collectors.toList());

        List<Section32> sections = new ArrayList<>();
        sections.add(Section32.builder().title("Supplies made to Unregistered Persons").rows(urpRows).build());
        sections.add(Section32.builder().title("Supplies made to Composition Taxable Persons").rows(new ArrayList<>()).build());
        sections.add(Section32.builder().title("Supplies made to UIN holders").rows(new ArrayList<>()).build());

        return Table32.builder()
                .title("3.2 Of the supplies shown in 3.1 (a) above, details of inter-State supplies made to unregistered persons, composition taxable persons and UIN holders")
                .headers(List.of("Place of Supply (State/UT)", "Total Taxable Value", "Amount of Integrated Tax"))
                .sections(sections)
                .build();
    }

    // ── Table 4 ──────────────────────────────────────────────────
    private Table4 buildTable4(Integer gstr3bFilingId, Integer gstr2Id) {
        List<Gstr3bItcSummary> summary = itcSummaryRepository.findByFiling_Id(gstr3bFilingId);
        boolean synced = !summary.isEmpty();

        Map<String, Gstr3bItcSummary> avl = categoryTotals(summary, "itcavl");
        Map<String, Gstr3bItcSummary> rejected = categoryTotals(summary, "itcRejected");

        // (A) ITC Available
        BigDecimal[] importGoods = synced ? amounts(avl.get("imports")) : fallbackImportGoods(gstr2Id);
        BigDecimal[] importServices = fallbackImportServices(gstr2Id); // TODO: no 2B category for this - always from purchase register
        BigDecimal[] rcmOther = synced ? amounts(avl.get("revsup")) : fallbackRcmOther(gstr2Id);
        BigDecimal[] isd = synced ? amounts(avl.get("isdsup")) : zeros4(); // TODO: no ISD sheet in purchase register schema yet
        BigDecimal[] allOther = synced ? amounts(avl.get("nonrevsup")) : fallbackAllOtherItc(gstr2Id);

        List<Row4> aRows = new ArrayList<>();
        aRows.add(row4("1", "(1) Import of goods", importGoods));
        aRows.add(row4("2", "(2) Import of services", importServices));
        aRows.add(row4("3", "(3) Inward supplies liable to reverse charge (other than 1 & 2 above)", rcmOther));
        aRows.add(row4("4", "(4) Inward supplies from ISD", isd));
        aRows.add(row4("5", "(5) All other ITC", allOther));

        BigDecimal[] aTotal = sum4(importGoods, importServices, rcmOther, isd, allOther);

        // (B) ITC Reversed - primarily from the purchase-register Gstr2Itcr ledger.
        // TODO: keyword match on free-text description to bucket rule 38/42/43/17(5)
        // vs "Others" - refine once the entity carries a structured reversal-reason code.
        BigDecimal[] rule3842 = zeros4(), others = zeros4();
        if (gstr2Id != null) {
            for (Gstr2Itcr r : gstr2Service.getItcr(gstr2Id)) {
                BigDecimal[] target = looksLikeRuleReversal(r.getDescriptionForReversalOfItc()) ? rule3842 : others;
                target[0] = target[0].add(nz(r.getItcIntegratedTaxAmount()));
                target[1] = target[1].add(nz(r.getItcCentralTaxAmount()));
                target[2] = target[2].add(nz(r.getItcStateUtTaxAmount()));
                target[3] = target[3].add(nz(r.getItcCessAmount()));
            }
        }
        List<Row4> bRows = new ArrayList<>();
        bRows.add(row4("1", "(1) As per rules 38, 42 and 43 of CGST Rules and section 17(5)", rule3842));
        bRows.add(row4("2", "(2) Others", others));
        BigDecimal[] bTotal = sum4(rule3842, others);

        // (C) Net ITC
        BigDecimal[] cVals = new BigDecimal[4];
        for (int i = 0; i < 4; i++) cVals[i] = aTotal[i].subtract(bTotal[i]);
        List<Row4> cRows = List.of(row4("", "Net ITC Available", cVals));

        // (D) Other details
        BigDecimal[] reclaimed = zeros4(); // TODO: not tracked - needs a reclaim ledger across periods
        BigDecimal[] ineligible = synced ? sumAllCategories(rejected) : zeros4();
        List<Row4> dRows = new ArrayList<>();
        dRows.add(row4("1", "(1) ITC reclaimed which was reversed under Table 4(B)(2) in earlier tax period", reclaimed));
        dRows.add(row4("2", "(2) Ineligible ITC under section 16(4) & ITC restricted due to PoS rules", ineligible));

        List<Section4> sections = List.of(
                Section4.builder().id("A").title("(A) ITC Available (whether in full or part)").rows(aRows).build(),
                Section4.builder().id("B").title("(B) ITC Reversed").rows(bRows).build(),
                Section4.builder().id("C").title("(C) Net ITC Available (A) - (B)").rows(cRows).build(),
                Section4.builder().id("D").title("(D) Other Details").rows(dRows).build()
        );

        return Table4.builder().title("4. Eligible ITC")
                .headers(List.of("Details", "Integrated Tax", "Central Tax", "State/UT Tax", "Cess"))
                .sections(sections).build();
    }

    private Row4 row4(String id, String detail, BigDecimal[] v) {
        return Row4.builder().id(id).detail(detail).integrated_tax(rnd(v[0])).central_tax(rnd(v[1]))
                .state_ut_tax(rnd(v[2])).cess(rnd(v[3])).build();
    }

    private Map<String, Gstr3bItcSummary> categoryTotals(List<Gstr3bItcSummary> rows, String bucket) {
        Map<String, Gstr3bItcSummary> map = new HashMap<>();
        for (Gstr3bItcSummary r : rows) {
            if (bucket.equals(r.getBucket()) && r.getSubCategory() == null) {
                map.put(r.getCategory(), r);
            }
        }
        return map;
    }

    private BigDecimal[] amounts(Gstr3bItcSummary r) {
        if (r == null) return zeros4();
        return new BigDecimal[]{nz(r.getIntegratedTax()), nz(r.getCentralTax()), nz(r.getStateUtTax()), nz(r.getCess())};
    }

    private BigDecimal[] sumAllCategories(Map<String, Gstr3bItcSummary> categoryMap) {
        BigDecimal[] tot = zeros4();
        for (Gstr3bItcSummary r : categoryMap.values()) {
            tot[0] = tot[0].add(nz(r.getIntegratedTax()));
            tot[1] = tot[1].add(nz(r.getCentralTax()));
            tot[2] = tot[2].add(nz(r.getStateUtTax()));
            tot[3] = tot[3].add(nz(r.getCess()));
        }
        return tot;
    }

    private BigDecimal[] fallbackImportGoods(Integer gstr2Id) {
        if (gstr2Id == null) return zeros4();
        BigDecimal igst = BigDecimal.ZERO, cess = BigDecimal.ZERO;
        for (Gstr2Impg r : gstr2Service.getImpg(gstr2Id)) {
            igst = igst.add(nz(r.getAvailedItcIntegratedTax()));
            cess = cess.add(nz(r.getAvailedItcCess()));
        }
        return new BigDecimal[]{igst, BigDecimal.ZERO, BigDecimal.ZERO, cess};
    }

    private BigDecimal[] fallbackImportServices(Integer gstr2Id) {
        if (gstr2Id == null) return zeros4();
        BigDecimal igst = BigDecimal.ZERO, cess = BigDecimal.ZERO;
        for (Gstr2Imps r : gstr2Service.getImps(gstr2Id)) {
            igst = igst.add(nz(r.getAvailedItcIntegratedTax()));
            cess = cess.add(nz(r.getAvailedItcCess()));
        }
        return new BigDecimal[]{igst, BigDecimal.ZERO, BigDecimal.ZERO, cess};
    }

    private BigDecimal[] fallbackRcmOther(Integer gstr2Id) {
        if (gstr2Id == null) return zeros4();
        BigDecimal[] tot = zeros4();
        for (Gstr2B2b r : gstr2Service.getB2b(gstr2Id)) {
            if (!"Y".equalsIgnoreCase(r.getReverseCharge())) continue;
            addItc(tot, r.getAvailedItcIntegratedTax(), r.getAvailedItcCentralTax(), r.getAvailedItcStateUtTax(), r.getAvailedItcCess());
        }
        return tot;
    }

    private BigDecimal[] fallbackAllOtherItc(Integer gstr2Id) {
        if (gstr2Id == null) return zeros4();
        BigDecimal[] tot = zeros4();
        for (Gstr2B2b r : gstr2Service.getB2b(gstr2Id)) {
            if ("Y".equalsIgnoreCase(r.getReverseCharge())) continue;
            addItc(tot, r.getAvailedItcIntegratedTax(), r.getAvailedItcCentralTax(), r.getAvailedItcStateUtTax(), r.getAvailedItcCess());
        }
        for (Gstr2Cdnr r : gstr2Service.getCdnr(gstr2Id)) {
            // No reverseCharge column on this sheet - all CDNR ITC is counted here.
            addItc(tot, r.getAvailedItcIntegratedTax(), r.getAvailedItcCentralTax(), r.getAvailedItcStateUtTax(), r.getAvailedItcCess());
        }
        for (Gstr2B2bur r : gstr2Service.getB2bur(gstr2Id)) {
            addItc(tot, r.getAvailedItcIntegratedTax(), r.getAvailedItcCentralTax(), r.getAvailedItcStateUtTax(), r.getAvailedItcCess());
        }
        for (Gstr2Cdnur r : gstr2Service.getCdnur(gstr2Id)) {
            addItc(tot, r.getAvailedItcIntegratedTax(), r.getAvailedItcCentralTax(), r.getAvailedItcStateUtTax(), r.getAvailedItcCess());
        }
        return tot;
    }

    private boolean looksLikeRuleReversal(String description) {
        if (description == null) return false;
        String d = description.toLowerCase();
        return d.contains("rule 38") || d.contains("rule 42") || d.contains("rule 43") || d.contains("17(5)") || d.contains("17-5");
    }

    // ── Table 5 ──────────────────────────────────────────────────
    /** TODO: Gstr2Exemp has no inter/intra-state flag, so totals are bucketed
     *  entirely under inter_state_supplies (intra = 0), same limitation noted
     *  for the equivalent GSTR-1 table 8. */
    private Table5 buildTable5(Integer gstr2Id) {
        BigDecimal compositionExemptNil = BigDecimal.ZERO, nonGst = BigDecimal.ZERO;
        if (gstr2Id != null) {
            for (Gstr2Exemp r : gstr2Service.getExemp(gstr2Id)) {
                compositionExemptNil = compositionExemptNil
                        .add(nz(r.getCompositionTaxablePerson())).add(nz(r.getNilRatedSupplies())).add(nz(r.getExemptedSupplies()));
                nonGst = nonGst.add(nz(r.getNonGstSupplies()));
            }
        }
        List<Row5> rows = List.of(
                Row5.builder().id("1").nature_of_supply("From a supplier under composition scheme, Exempt and Nil rated supply")
                        .inter_state_supplies(rnd(compositionExemptNil)).intra_state_supplies(BigDecimal.ZERO.setScale(2)).build(),
                Row5.builder().id("2").nature_of_supply("Non GST supply")
                        .inter_state_supplies(rnd(nonGst)).intra_state_supplies(BigDecimal.ZERO.setScale(2)).build()
        );
        return Table5.builder().title("5. Values of exempt, nil-rated and non-GST inward supplies")
                .headers(List.of("Nature of supplies", "Inter-State supplies", "Intra-State supplies"))
                .rows(rows).build();
    }

    // ── Table 5.1 ────────────────────────────────────────────────
    private Table51 buildTable51(Gstr3bFiling filing) {
        List<Row51> rows = List.of(
                Row51.builder().id("1").description("Interest")
                        .integrated_tax(rnd(filing.getInterestIntegratedTax())).central_tax(rnd(filing.getInterestCentralTax()))
                        .state_ut_tax(rnd(filing.getInterestStateUtTax())).cess(rnd(filing.getInterestCess())).build(),
                Row51.builder().id("2").description("Late Fee")
                        .integrated_tax(BigDecimal.ZERO.setScale(2)).central_tax(rnd(filing.getLateFeeCentralTax()))
                        .state_ut_tax(rnd(filing.getLateFeeStateUtTax())).cess(BigDecimal.ZERO.setScale(2)).build()
        );
        return Table51.builder().title("5.1 Interest and Late Fee")
                .headers(List.of("Description", "Integrated Tax", "Central Tax", "State/UT Tax", "Cess"))
                .rows(rows).build();
    }

    // ── Table 6.1 ────────────────────────────────────────────────
    /** Same-head-only ITC offset (IGST credit against IGST payable, etc.).
     *  TODO: real cross-utilization rules (IGST credit can offset CGST/SGST
     *  shortfall before cash) are not implemented - refine once challan/payment
     *  logic is built. tax_paid_tds_tcs is always 0 (not tracked here). */
    private Table61 buildTable61(Table31 t31, Table4 t4, Table51 t51) {
        BigDecimal payableIgst = t31.getTotal().getIntegrated_tax();
        BigDecimal payableCgst = t31.getTotal().getCentral_tax();
        BigDecimal payableSgst = t31.getTotal().getState_ut_tax();
        BigDecimal payableCess = t31.getTotal().getCess();

        Row4 netItc = t4.getSections().stream()
                .filter(s -> "C".equals(s.getId())).findFirst()
                .map(s -> s.getRows().get(0))
                .orElse(Row4.builder().integrated_tax(BigDecimal.ZERO).central_tax(BigDecimal.ZERO)
                        .state_ut_tax(BigDecimal.ZERO).cess(BigDecimal.ZERO).build());

        Row51 interest = t51.getRows().get(0);
        Row51 lateFee = t51.getRows().get(1);

        List<Row61> rows = List.of(
                payRow("Integrated Tax", payableIgst, netItc.getIntegrated_tax(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        interest.getIntegrated_tax(), lateFee.getIntegrated_tax()),
                payRow("Central Tax", payableCgst, BigDecimal.ZERO, netItc.getCentral_tax(), BigDecimal.ZERO, BigDecimal.ZERO,
                        interest.getCentral_tax(), lateFee.getCentral_tax()),
                payRow("State/UT Tax", payableSgst, BigDecimal.ZERO, BigDecimal.ZERO, netItc.getState_ut_tax(), BigDecimal.ZERO,
                        interest.getState_ut_tax(), lateFee.getState_ut_tax()),
                payRow("Cess", payableCess, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, netItc.getCess(),
                        interest.getCess(), lateFee.getCess())
        );

        return Table61.builder().title("6.1 Payment of tax")
                .headers(List.of("Description", "Tax Payable", "Paid through ITC - Integrated Tax",
                        "Paid through ITC - Central Tax", "Paid through ITC - State/UT Tax", "Paid through ITC - Cess",
                        "Tax paid TDS/TCS", "Tax/Cess paid in cash", "Interest paid in cash", "Late Fee paid in cash"))
                .rows(rows).build();
    }

    private Row61 payRow(String desc, BigDecimal payable, BigDecimal itcI, BigDecimal itcC, BigDecimal itcS, BigDecimal itcCess,
                         BigDecimal interest, BigDecimal lateFee) {
        BigDecimal payableNz = nz(payable);
        BigDecimal itcUsed = nz(itcI).add(nz(itcC)).add(nz(itcS)).add(nz(itcCess)).min(payableNz);
        BigDecimal cash = payableNz.subtract(itcUsed).max(BigDecimal.ZERO);
        return Row61.builder().description(desc).tax_payable(rnd(payableNz))
                .paid_itc_integrated(rnd(itcI)).paid_itc_central(rnd(itcC))
                .paid_itc_state_ut(rnd(itcS)).paid_itc_cess(rnd(itcCess))
                .tax_paid_tds_tcs(BigDecimal.ZERO.setScale(2))
                .tax_paid_cash(rnd(cash)).interest_paid_cash(rnd(interest)).late_fee_paid_cash(rnd(lateFee)).build();
    }

    // ── Shared helpers ───────────────────────────────────────────

    private boolean isRegularInvoiceType(String invoiceType) {
        return invoiceType == null || invoiceType.isBlank() || invoiceType.equalsIgnoreCase("Regular");
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private boolean isIntra(String pos, String homeState) {
        return homeState != null && homeState.equals(stateCode(pos));
    }

    private String stateCode(String posOrGstin) {
        if (posOrGstin == null || posOrGstin.length() < 2) return null;
        return posOrGstin.substring(0, 2);
    }

    private BigDecimal taxAmt(BigDecimal taxableValue, BigDecimal rate) {
        return nz(taxableValue).multiply(nz(rate)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void add(BigDecimal[] tot, BigDecimal taxableValue, BigDecimal rate, String pos, String homeState, BigDecimal cess) {
        BigDecimal tax = taxAmt(taxableValue, rate);
        tot[0] = tot[0].add(nz(taxableValue));
        if (isIntra(pos, homeState)) {
            BigDecimal half = tax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            tot[2] = tot[2].add(half);
            tot[3] = tot[3].add(half);
        } else {
            tot[1] = tot[1].add(tax);
        }
        tot[4] = tot[4].add(nz(cess));
    }

    private void addItc(BigDecimal[] tot, BigDecimal igst, BigDecimal cgst, BigDecimal sgst, BigDecimal cess) {
        tot[0] = tot[0].add(nz(igst));
        tot[1] = tot[1].add(nz(cgst));
        tot[2] = tot[2].add(nz(sgst));
        tot[3] = tot[3].add(nz(cess));
    }

    private BigDecimal[] sum4(BigDecimal[]... arrays) {
        BigDecimal[] tot = zeros4();
        for (BigDecimal[] a : arrays) {
            for (int i = 0; i < 4; i++) tot[i] = tot[i].add(nz(a[i]));
        }
        return tot;
    }

    private BigDecimal[] zeros5() { return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO}; }
    private BigDecimal[] zeros4() { return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO}; }

    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private BigDecimal rnd(BigDecimal v) { return v == null ? null : v.setScale(2, RoundingMode.HALF_UP); }
}