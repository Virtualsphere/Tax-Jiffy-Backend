package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mirrors the exact JSON body accepted by PUT /gstr1/retsave ("Used to save
 * entire GSTR1 invoices."). Field names use @JsonProperty to keep the wire
 * format matching the government schema while Java stays camelCase.
 */
@Data
public class WhitebooksGstr1Payload {

    @JsonProperty("gstin")  private String gstin;
    @JsonProperty("fp")     private String fp;       // return period, MMYYYY
    @JsonProperty("gt")     private BigDecimal gt;    // gross turnover (preceding FY) - see class TODO
    @JsonProperty("cur_gt") private BigDecimal curGt; // current gross turnover - see class TODO

    @JsonProperty("b2b")   private List<B2bEntry> b2b;
    @JsonProperty("b2ba")  private List<B2baEntry> b2ba;
    @JsonProperty("b2cl")  private List<B2clEntry> b2cl;
    @JsonProperty("b2cla") private List<B2claEntry> b2cla;
    @JsonProperty("cdnr")  private List<CdnrEntry> cdnr;
    @JsonProperty("cdnra") private List<CdnraEntry> cdnra;
    @JsonProperty("b2cs")  private List<B2csRow> b2cs;
    @JsonProperty("b2csa") private List<B2csaEntry> b2csa;
    @JsonProperty("exp")   private List<ExpEntry> exp;
    @JsonProperty("expa")  private List<ExpaEntry> expa;
    @JsonProperty("hsn")   private Hsn hsn;
    @JsonProperty("nil")   private NilBlock nil;
    @JsonProperty("txpd")  private List<AdvanceEntry> txpd;
    @JsonProperty("txpda") private List<AdvanceAmendEntry> txpda;
    @JsonProperty("at")    private List<AdvanceEntry> at;
    @JsonProperty("ata")   private List<AdvanceAmendEntry> ata;
    @JsonProperty("doc_issue") private DocIssue docIssue;
    @JsonProperty("cdnur")  private List<CdnurEntry> cdnur;
    @JsonProperty("cdnura") private List<CdnuraEntry> cdnura;

    // ── Shared item shapes ───────────────────────────────────────
    @Data
    public static class ItemDetail {
        @JsonProperty("rt")    private BigDecimal rt;
        @JsonProperty("txval") private BigDecimal txval;
        @JsonProperty("iamt")  private BigDecimal iamt;
        @JsonProperty("camt")  private BigDecimal camt;
        @JsonProperty("samt")  private BigDecimal samt;
        @JsonProperty("csamt") private BigDecimal csamt;
    }

    /** {num, itm_det} wrapper used by b2b, b2ba, b2cl, b2cla, cdnr, cdnra, cdnur, cdnura. */
    @Data
    public static class Item {
        @JsonProperty("num")     private Integer num;
        @JsonProperty("itm_det") private ItemDetail itmDet;
    }

    /** Flat (non-wrapped) tax row used by b2csa itms. */
    @Data
    public static class FlatTaxItem {
        @JsonProperty("rt")    private BigDecimal rt;
        @JsonProperty("txval") private BigDecimal txval;
        @JsonProperty("iamt")  private BigDecimal iamt;
        @JsonProperty("camt")  private BigDecimal camt;
        @JsonProperty("samt")  private BigDecimal samt;
        @JsonProperty("csamt") private BigDecimal csamt;
    }

    /** Flat export item (exp/expa) - always IGST, no camt/samt. */
    @Data
    public static class ExpItem {
        @JsonProperty("txval") private BigDecimal txval;
        @JsonProperty("rt")    private BigDecimal rt;
        @JsonProperty("iamt")  private BigDecimal iamt;
        @JsonProperty("csamt") private BigDecimal csamt;
    }

    /** Advance received/adjusted item (at, ata, txpd, txpda). */
    @Data
    public static class AdvanceItem {
        @JsonProperty("rt")     private BigDecimal rt;
        @JsonProperty("ad_amt") private BigDecimal adAmt;
        @JsonProperty("iamt")   private BigDecimal iamt;
        @JsonProperty("camt")   private BigDecimal camt;
        @JsonProperty("samt")   private BigDecimal samt;
        @JsonProperty("csamt")  private BigDecimal csamt;
    }

    // ── B2B / B2BA ───────────────────────────────────────────────
    @Data
    public static class B2bInvoice {
        @JsonProperty("inum")         private String inum;
        @JsonProperty("idt")          private String idt;
        @JsonProperty("val")          private BigDecimal val;
        @JsonProperty("pos")          private String pos;
        @JsonProperty("rchrg")        private String rchrg;
        @JsonProperty("etin")         private String etin;
        @JsonProperty("inv_typ")      private String invTyp;
        @JsonProperty("diff_percent") private BigDecimal diffPercent;
        @JsonProperty("itms")         private List<Item> itms;
    }

    @Data
    public static class B2baInvoice extends B2bInvoice {
        @JsonProperty("oinum") private String oinum;
        @JsonProperty("oidt")  private String oidt;
    }

    @Data
    public static class B2bEntry {
        @JsonProperty("ctin") private String ctin;
        @JsonProperty("inv")  private List<B2bInvoice> inv;
    }

    @Data
    public static class B2baEntry {
        @JsonProperty("ctin") private String ctin;
        @JsonProperty("inv")  private List<B2baInvoice> inv;
    }

    // ── B2CL / B2CLA ─────────────────────────────────────────────
    @Data
    public static class B2clInvoice {
        @JsonProperty("inum")         private String inum;
        @JsonProperty("idt")          private String idt;
        @JsonProperty("val")          private BigDecimal val;
        @JsonProperty("inv_typ")      private String invTyp;
        @JsonProperty("etin")         private String etin;
        @JsonProperty("diff_percent") private BigDecimal diffPercent;
        @JsonProperty("itms")         private List<Item> itms;
    }

    @Data
    public static class B2claInvoice extends B2clInvoice {
        @JsonProperty("oinum") private String oinum;
        @JsonProperty("oidt")  private String oidt;
    }

    @Data
    public static class B2clEntry {
        @JsonProperty("pos") private String pos;
        @JsonProperty("inv") private List<B2clInvoice> inv;
    }

    @Data
    public static class B2claEntry {
        @JsonProperty("pos") private String pos;
        @JsonProperty("inv") private List<B2claInvoice> inv;
    }

    // ── CDNR / CDNRA ─────────────────────────────────────────────
    @Data
    public static class CdnrNote {
        @JsonProperty("ntty")         private String ntty;
        @JsonProperty("nt_num")       private String ntNum;
        @JsonProperty("nt_dt")        private String ntDt;
        @JsonProperty("p_gst")        private String pGst;
        @JsonProperty("pos")          private String pos;
        @JsonProperty("rchrg")        private String rchrg;
        @JsonProperty("inv_typ")      private String invTyp;
        @JsonProperty("val")          private BigDecimal val;
        @JsonProperty("diff_percent") private BigDecimal diffPercent;
        @JsonProperty("itms")         private List<Item> itms;
    }

    @Data
    public static class CdnraNote extends CdnrNote {
        @JsonProperty("ont_num") private String ontNum;
        @JsonProperty("ont_dt")  private String ontDt;
    }

    @Data
    public static class CdnrEntry {
        @JsonProperty("ctin") private String ctin;
        @JsonProperty("nt")   private List<CdnrNote> nt;
    }

    @Data
    public static class CdnraEntry {
        @JsonProperty("ctin") private String ctin;
        @JsonProperty("nt")   private List<CdnraNote> nt;
    }

    // ── B2CS (flat) / B2CSA ──────────────────────────────────────
    @Data
    public static class B2csRow {
        @JsonProperty("sply_ty")      private String splyTy;
        @JsonProperty("diff_percent") private BigDecimal diffPercent;
        @JsonProperty("rt")           private BigDecimal rt;
        @JsonProperty("typ")          private String typ;
        @JsonProperty("etin")         private String etin;
        @JsonProperty("pos")          private String pos;
        @JsonProperty("txval")        private BigDecimal txval;
        @JsonProperty("iamt")         private BigDecimal iamt;
        @JsonProperty("csamt")        private BigDecimal csamt;
    }

    @Data
    public static class B2csaEntry {
        @JsonProperty("omon")         private String omon;
        @JsonProperty("sply_ty")      private String splyTy;
        @JsonProperty("diff_percent") private BigDecimal diffPercent;
        @JsonProperty("typ")          private String typ;
        @JsonProperty("etin")         private String etin;
        @JsonProperty("pos")          private String pos;
        @JsonProperty("itms")         private List<FlatTaxItem> itms;
    }

    // ── EXP / EXPA ───────────────────────────────────────────────
    @Data
    public static class ExpInvoice {
        @JsonProperty("inum")         private String inum;
        @JsonProperty("idt")          private String idt;
        @JsonProperty("val")          private BigDecimal val;
        @JsonProperty("diff_percent") private BigDecimal diffPercent;
        @JsonProperty("sbpcode")      private String sbpcode;
        @JsonProperty("sbnum")        private String sbnum;
        @JsonProperty("sbdt")         private String sbdt;
        @JsonProperty("itms")         private List<ExpItem> itms;
    }

    @Data
    public static class ExpaInvoice extends ExpInvoice {
        @JsonProperty("oinum") private String oinum;
        @JsonProperty("oidt")  private String oidt;
    }

    @Data
    public static class ExpEntry {
        @JsonProperty("exp_typ") private String expTyp;
        @JsonProperty("inv")     private List<ExpInvoice> inv;
    }

    @Data
    public static class ExpaEntry {
        @JsonProperty("exp_typ") private String expTyp;
        @JsonProperty("inv")     private List<ExpaInvoice> inv;
    }

    // ── HSN ──────────────────────────────────────────────────────
    @Data
    public static class HsnRow {
        @JsonProperty("num")    private Integer num;
        @JsonProperty("hsn_sc") private String hsnSc;
        @JsonProperty("desc")   private String desc;
        @JsonProperty("uqc")    private String uqc;
        @JsonProperty("qty")    private BigDecimal qty;
        @JsonProperty("rt")     private BigDecimal rt;
        @JsonProperty("txval")  private BigDecimal txval;
        @JsonProperty("iamt")   private BigDecimal iamt;
        @JsonProperty("csamt")  private BigDecimal csamt;
    }

    @Data
    public static class Hsn {
        @JsonProperty("data") private List<HsnRow> data;
    }

    // ── NIL ──────────────────────────────────────────────────────
    @Data
    public static class NilRow {
        @JsonProperty("sply_ty")   private String splyTy;
        @JsonProperty("expt_amt")  private BigDecimal exptAmt;
        @JsonProperty("nil_amt")   private BigDecimal nilAmt;
        @JsonProperty("ngsup_amt") private BigDecimal ngsupAmt;
    }

    @Data
    public static class NilBlock {
        @JsonProperty("inv") private List<NilRow> inv;
    }

    // ── AT / ATA / TXPD / TXPDA ──────────────────────────────────
    @Data
    public static class AdvanceEntry {
        @JsonProperty("pos")          private String pos;
        @JsonProperty("sply_ty")      private String splyTy;
        @JsonProperty("diff_percent") private BigDecimal diffPercent;
        @JsonProperty("itms")         private List<AdvanceItem> itms;
    }

    @Data
    public static class AdvanceAmendEntry extends AdvanceEntry {
        @JsonProperty("omon") private String omon;
    }

    // ── DOC ISSUE ────────────────────────────────────────────────
    @Data
    public static class DocDetailRow {
        @JsonProperty("num")       private Integer num;
        @JsonProperty("from")      private String from;
        @JsonProperty("to")        private String to;
        @JsonProperty("totnum")    private Integer totnum;
        @JsonProperty("cancel")    private Integer cancel;
        @JsonProperty("net_issue") private Integer netIssue;
    }

    @Data
    public static class DocDet {
        @JsonProperty("doc_num") private Integer docNum;
        @JsonProperty("docs")    private List<DocDetailRow> docs;
    }

    @Data
    public static class DocIssue {
        @JsonProperty("doc_det") private List<DocDet> docDet;
    }

    // ── CDNUR / CDNURA ───────────────────────────────────────────
    @Data
    public static class CdnurEntry {
        @JsonProperty("typ")          private String typ;
        @JsonProperty("ntty")         private String ntty;
        @JsonProperty("nt_num")       private String ntNum;
        @JsonProperty("nt_dt")        private String ntDt;
        @JsonProperty("p_gst")        private String pGst;
        @JsonProperty("pos")          private String pos;
        @JsonProperty("val")          private BigDecimal val;
        @JsonProperty("diff_percent") private BigDecimal diffPercent;
        @JsonProperty("itms")         private List<Item> itms;
    }

    @Data
    public static class CdnuraEntry {
        @JsonProperty("ont_num")      private String ontNum;
        @JsonProperty("ont_dt")       private String ontDt;
        @JsonProperty("nt_num")       private String ntNum;
        @JsonProperty("nt_dt")        private String ntDt;
        @JsonProperty("ntty")         private String ntty;
        @JsonProperty("typ")          private String typ;
        @JsonProperty("p_gst")        private String pGst;
        @JsonProperty("inum")         private String inum;
        @JsonProperty("val")          private BigDecimal val;
        @JsonProperty("idt")          private String idt;
        @JsonProperty("diff_percent") private BigDecimal diffPercent;
        @JsonProperty("itms")         private List<Item> itms;
    }
}