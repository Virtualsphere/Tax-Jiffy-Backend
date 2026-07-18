package com.gst_reconsilation.gstr3b.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mirrors the response of GET /ims/supplierinvoices. Both "gstr1" (as originally
 * filed) and "gstr1a" (post-filing amendment window) carry the exact same shape.
 */
@Data
public class ImsApiResponse {
    @JsonProperty("gstin")  private String gstin;
    @JsonProperty("rtnprd") private String rtnprd;
    @JsonProperty("gstr1")  private ImsSection gstr1;
    @JsonProperty("gstr1a") private ImsSection gstr1a;

    @Data
    public static class ImsSection {
        @JsonProperty("b2b")   private List<ImsB2bEntry> b2b;
        @JsonProperty("b2ba")  private List<ImsB2baEntry> b2ba;
        @JsonProperty("cdnr")  private List<ImsCdnrEntry> cdnr;
        @JsonProperty("cdnra") private List<ImsCdnraEntry> cdnra;
        @JsonProperty("ecom")  private ImsEcomBlock ecom;
        @JsonProperty("ecoma") private ImsEcomaBlock ecoma;
    }

    @Data
    public static class ImsItemDetail {
        @JsonProperty("rt")    private BigDecimal rate;
        @JsonProperty("txval") private BigDecimal taxableValue;
        @JsonProperty("iamt")  private BigDecimal iamt;
        @JsonProperty("camt")  private BigDecimal camt;
        @JsonProperty("samt")  private BigDecimal samt;
        @JsonProperty("csamt") private BigDecimal csamt;
    }

    @Data
    public static class ImsItem {
        @JsonProperty("num")     private Integer num;
        @JsonProperty("itm_det") private ImsItemDetail detail;
    }

    @Data
    public static class ImsB2bInvoice {
        @JsonProperty("inum")     private String invoiceNumber;
        @JsonProperty("idt")      private String invoiceDate;
        @JsonProperty("val")      private BigDecimal invoiceValue;
        @JsonProperty("pos")      private String pos;
        @JsonProperty("rchrg")    private String reverseCharge;
        @JsonProperty("etin")     private String etin;
        @JsonProperty("inv_typ")  private String invoiceType;
        @JsonProperty("imsactn")  private String imsAction;
        @JsonProperty("remarks")  private String remarks;
        @JsonProperty("itms")     private List<ImsItem> itms;
    }

    @Data
    public static class ImsB2baInvoice extends ImsB2bInvoice {
        @JsonProperty("oinum") private String originalInvoiceNumber;
        @JsonProperty("oidt")  private String originalInvoiceDate;
    }

    @Data
    public static class ImsB2bEntry {
        @JsonProperty("ctin") private String ctin;
        @JsonProperty("cfs")  private String cfs;
        @JsonProperty("inv")  private List<ImsB2bInvoice> inv;
    }

    @Data
    public static class ImsB2baEntry {
        @JsonProperty("ctin") private String ctin;
        @JsonProperty("cfs")  private String cfs;
        @JsonProperty("inv")  private List<ImsB2baInvoice> inv;
    }

    @Data
    public static class ImsCdnrNote {
        @JsonProperty("nt_num")  private String noteNumber;
        @JsonProperty("nt_dt")   private String noteDate;
        @JsonProperty("inum")    private String invoiceNumber;
        @JsonProperty("idt")     private String invoiceDate;
        @JsonProperty("val")     private BigDecimal noteValue;
        @JsonProperty("pos")     private String pos;
        @JsonProperty("rchrg")   private String reverseCharge;
        @JsonProperty("inv_typ") private String invoiceType;
        @JsonProperty("imsactn") private String imsAction;
        @JsonProperty("remarks") private String remarks;
        @JsonProperty("itms")    private List<ImsItem> itms;
    }

    @Data
    public static class ImsCdnraNote extends ImsCdnrNote {
        @JsonProperty("ont_num") private String originalNoteNumber;
        @JsonProperty("ont_dt")  private String originalNoteDate;
    }

    @Data
    public static class ImsCdnrEntry {
        @JsonProperty("ctin") private String ctin;
        @JsonProperty("cfs")  private String cfs;
        @JsonProperty("nt")   private List<ImsCdnrNote> nt;
    }

    @Data
    public static class ImsCdnraEntry {
        @JsonProperty("ctin") private String ctin;
        @JsonProperty("cfs")  private String cfs;
        @JsonProperty("nt")   private List<ImsCdnraNote> nt;
    }

    @Data
    public static class ImsEcomInvoice {
        @JsonProperty("val")     private BigDecimal invoiceValue;
        @JsonProperty("itms")    private List<ImsItem> itms;
        @JsonProperty("inv_typ") private String invoiceType;
        @JsonProperty("pos")     private String pos;
        @JsonProperty("idt")     private String invoiceDate;
        @JsonProperty("inum")    private String invoiceNumber;
        @JsonProperty("sply_ty") private String supplyType;
        @JsonProperty("imsactn") private String imsAction;
        @JsonProperty("remarks") private String remarks;
    }

    @Data
    public static class ImsEcomaInvoice extends ImsEcomInvoice {
        @JsonProperty("oinum") private String originalInvoiceNumber;
        @JsonProperty("oidt")  private String originalInvoiceDate;
    }

    @Data
    public static class ImsEcomB2bEntry {
        @JsonProperty("inv")  private List<ImsEcomInvoice> inv;
        @JsonProperty("rtin") private String rtin; // recipient GSTIN
        @JsonProperty("stin") private String stin; // e-commerce operator GSTIN
    }

    @Data
    public static class ImsEcomUrp2bEntry {
        @JsonProperty("inv")  private List<ImsEcomInvoice> inv;
        @JsonProperty("rtin") private String rtin;
    }

    @Data
    public static class ImsEcomaB2baEntry {
        @JsonProperty("inv")  private List<ImsEcomaInvoice> inv;
        @JsonProperty("rtin") private String rtin;
        @JsonProperty("stin") private String stin;
    }

    @Data
    public static class ImsEcomaUrp2baEntry {
        @JsonProperty("inv")  private List<ImsEcomaInvoice> inv;
        @JsonProperty("rtin") private String rtin;
    }

    @Data
    public static class ImsEcomBlock {
        @JsonProperty("b2b")   private List<ImsEcomB2bEntry> b2b;
        @JsonProperty("urp2b") private List<ImsEcomUrp2bEntry> urp2b;
    }

    @Data
    public static class ImsEcomaBlock {
        @JsonProperty("b2ba")   private List<ImsEcomaB2baEntry> b2ba;
        @JsonProperty("urp2ba") private List<ImsEcomaUrp2baEntry> urp2ba;
    }
}