package com.gst_reconsilation.gstr3b.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Matches the frontend preview shape exactly (top-level key "gstr3b" wrapping
 * table_3_1 ... table_6_1). Fields use BigDecimal (nullable) so rows like
 * "Nil rated/exempted" and "Non-GST outward supplies" can emit JSON {@code null}
 * for tax columns, same as the sample.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr3bPreviewResponse {

    private Gstr3b gstr3b;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Gstr3b {
        private Table31 table_3_1_outward_and_reverse_charge_inward_supplies;
        private Table32 table_3_2_interstate_supplies;
        private Table4 table_4_eligible_itc;
        private Table5 table_5_exempt_nil_nongst_inward_supplies;
        private Table51 table_5_1_interest_and_late_fee;
        private Table61 table_6_1_payment_of_tax;
    }

    // ── 3.1 ──────────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table31 {
        private String title;
        @Builder.Default private List<String> headers = new ArrayList<>();
        @Builder.Default private List<Row31> rows = new ArrayList<>();
        private Total31 total;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Row31 {
        private String id;
        private String nature_of_supply;
        private BigDecimal taxable_value;
        private BigDecimal integrated_tax;
        private BigDecimal central_tax;
        private BigDecimal state_ut_tax;
        private BigDecimal cess;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Total31 {
        private BigDecimal taxable_value;
        private BigDecimal integrated_tax;
        private BigDecimal central_tax;
        private BigDecimal state_ut_tax;
        private BigDecimal cess;
    }

    // ── 3.2 ──────────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table32 {
        private String title;
        @Builder.Default private List<String> headers = new ArrayList<>();
        @Builder.Default private List<Section32> sections = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Section32 {
        private String title;
        @Builder.Default private List<Row32> rows = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Row32 {
        private String place_of_supply;
        private BigDecimal taxable_value;
        private BigDecimal integrated_tax;
    }

    // ── 4 ────────────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table4 {
        private String title;
        @Builder.Default private List<String> headers = new ArrayList<>();
        @Builder.Default private List<Section4> sections = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Section4 {
        private String id;
        private String title;
        @Builder.Default private List<Row4> rows = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Row4 {
        private String id;
        private String detail;
        private BigDecimal integrated_tax;
        private BigDecimal central_tax;
        private BigDecimal state_ut_tax;
        private BigDecimal cess;
    }

    // ── 5 ────────────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table5 {
        private String title;
        @Builder.Default private List<String> headers = new ArrayList<>();
        @Builder.Default private List<Row5> rows = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Row5 {
        private String id;
        private String nature_of_supply;
        private BigDecimal inter_state_supplies;
        private BigDecimal intra_state_supplies;
    }

    // ── 5.1 ──────────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table51 {
        private String title;
        @Builder.Default private List<String> headers = new ArrayList<>();
        @Builder.Default private List<Row51> rows = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Row51 {
        private String id;
        private String description;
        private BigDecimal integrated_tax;
        private BigDecimal central_tax;
        private BigDecimal state_ut_tax;
        private BigDecimal cess;
    }

    // ── 6.1 ──────────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Table61 {
        private String title;
        @Builder.Default private List<String> headers = new ArrayList<>();
        @Builder.Default private List<Row61> rows = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Row61 {
        private String description;
        private BigDecimal tax_payable;
        private BigDecimal paid_itc_integrated;
        private BigDecimal paid_itc_central;
        private BigDecimal paid_itc_state_ut;
        private BigDecimal paid_itc_cess;
        private BigDecimal tax_paid_tds_tcs;
        private BigDecimal tax_paid_cash;
        private BigDecimal interest_paid_cash;
        private BigDecimal late_fee_paid_cash;
    }
}