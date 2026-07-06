package com.gst_reconsilation.gstr1.dto.report;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Full GSTR-1 style report DTO returned by GET /api/gstr1/filings/{filingId}/report
 * Shape mirrors the standard GSTR-1 summary export (basicData / outwardData / amendmentsData /
 * advancedData / othersData).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr1ReportResponse {

    private List<BasicDataItem> basicData;
    private OutwardData outwardData;
    private AmendmentsData amendmentsData;
    private AdvancedData advancedData;
    private OthersData othersData;

    // ── Basic ───────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BasicDataItem {
        private String sr;
        private String label;
        private String sub;
        private String value;
        private boolean highlight;
    }

    // ── Outward (Table 4,5,6,7,8) ──────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OutwardData {
        private Table4 table4;
        private Table5 table5;
        private Table6 table6;
        private Table7 table7;
        private Table8 table8;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table4 {
        @Builder.Default private List<B2bRow> section4A = new ArrayList<>();
        @Builder.Default private List<B2bRow> section4B = new ArrayList<>();
        private String section4C_ecommerceGstin;
        @Builder.Default private List<B2bRow> section4C = new ArrayList<>();
        private Totals total;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class B2bRow {
        private String gstin;
        private String invoiceNo;
        private String invoiceDate;
        private String invoiceValue;
        private String taxableValue;
        private String igst;
        private String cgst;
        private String sgst;
        private String cess;
        private String pos;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Totals {
        private String invoiceValue;
        private String taxableValue;
        private String igst;
        private String cgst;
        private String sgst;
        private String cess;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table5 {
        @Builder.Default private List<B2clRow> section5A = new ArrayList<>();
        private String section5B_ecommerceGstin;
        @Builder.Default private List<B2clRow> section5B = new ArrayList<>();
        private Totals total;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class B2clRow {
        private String gstin;
        private String pos;
        private String invoiceNo;
        private String invoiceDate;
        private String invoiceValue;
        private String rate;
        private String taxableValue;
        private String igst;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table6 {
        @Builder.Default private List<ExportRow> section6A = new ArrayList<>();
        @Builder.Default private List<ExportRow> section6B = new ArrayList<>();
        @Builder.Default private List<ExportRow> section6C = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExportRow {
        private String gstin;
        private String invoiceNo;
        private String invoiceDate;
        private String invoiceValue;
        private String sbNo;
        private String sbDate;
        private String rate;
        private String taxableValue;
        private String amt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table7 {
        @Builder.Default private List<B2csRow> section7A1 = new ArrayList<>();
        private String section7A2_ecommerceGstin;
        @Builder.Default private List<B2csRow> section7A2 = new ArrayList<>();
        private String section7B1_pos;
        @Builder.Default private List<B2csRow> section7B1 = new ArrayList<>();
        private String section7B2_ecommerceGstin;
        @Builder.Default private List<B2csRow> section7B2 = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class B2csRow {
        private String rate;
        private String stateName;
        private String taxableValue;
        private String integrated;
        private String central;
        private String state;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table8 {
        private ExemptRow section8A;
        private ExemptRow section8B;
        private ExemptRow section8C;
        private ExemptRow section8D;
        private ExemptRow total;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExemptRow {
        private String label;
        private String nilRated;
        private String exempted;
        private String nonGst;
    }

    // ── Amendments (Table 9,10) ─────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AmendmentsData {
        private Table9 table9;
        private Table10 table10;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table9 {
        @Builder.Default private List<AmendRow> section9A = new ArrayList<>();
        @Builder.Default private List<AmendRow> section9B = new ArrayList<>();
        @Builder.Default private List<AmendRow> section9C = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AmendRow {
        private String originalGstin;
        private String originalInvNo;
        private String originalInvDate;
        private String revisedGstin;
        private String revisedInvNo;
        private String revisedInvDate;
        private String sbNo;
        private String sbDate;
        private String value;
        private String rate;
        private String taxableValue;
        private String integratedTax;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table10 {
        @Builder.Default private List<B2csRow> section10A = new ArrayList<>();
        private String section10A1_ecommerceGstin;
        @Builder.Default private List<B2csRow> section10A1 = new ArrayList<>();
        private String section10B_pos;
        @Builder.Default private List<B2csRow> section10B = new ArrayList<>();
        private String section10B1_ecommerceGstin;
        @Builder.Default private List<B2csRow> section10B1 = new ArrayList<>();
    }

    // ── Advances (Table 11) ─────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AdvancedData {
        private Table11 table11;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table11 {
        @Builder.Default private List<AdvanceRow> section11A1 = new ArrayList<>();
        @Builder.Default private List<AdvanceRow> section11A2 = new ArrayList<>();
        @Builder.Default private List<AdvanceRow> section11B1 = new ArrayList<>();
        @Builder.Default private List<AdvanceRow> section11B2 = new ArrayList<>();
        @Builder.Default private List<AdvanceAmendRow> amendments = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AdvanceRow {
        private String rate;
        private String grossAdvance;
        private String pos;
        private String integrated;
        private String central;
        private String state;
        private String cess;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AdvanceAmendRow {
        private String month;
        private String amendmentRelatingTo;
        private String val11A1;
        private String val11A2;
        private String val11B1;
        private String val11B2;
    }

    // ── Others (Table 12,13) ────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OthersData {
        private Table12 table12;
        private Table13 table13;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table12 {
        @Builder.Default private List<HsnRow> records = new ArrayList<>();
        private HsnTotal total;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HsnRow {
        private String hsn;
        private String description;
        private String uqc;
        private String totalQuantity;
        private String totalValue;
        private String taxableValue;
        private String integratedTax;
        private String centralTax;
        private String stateTax;
        private String cess;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HsnTotal {
        private String totalQuantity;
        private String totalValue;
        private String taxableValue;
        private String integratedTax;
        private String centralTax;
        private String stateTax;
        private String cess;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table13 {
        @Builder.Default private List<DocRow> records = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocRow {
        private String natureOfDocument;
        private String from;
        private String to;
        private String totalNumber;
        private String cancelled;
        private String netIssued;
    }
}