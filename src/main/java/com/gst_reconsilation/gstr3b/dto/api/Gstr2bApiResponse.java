package com.gst_reconsilation.gstr3b.dto.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Mirrors the response of GET /gstr2b/all - itcsumm block only.
 *
 * NOTE (scope): the full GSTR-2B payload also carries cpsumm (counter-party wise
 * summary), docdata / docRejdata (invoice-level detail) and cpSummRej. Those are
 * intentionally NOT modelled here - table 4 of the GSTR-3B preview only needs the
 * category-level totals in itcsumm to reconcile ITC available/unavailable/reversed/
 * rejected. If line-item (invoice-level) matching against the purchase register is
 * needed later, extend this DTO with docdata/docRejdata and a matching service.
 */
@Data
public class Gstr2bApiResponse {
    @JsonProperty("chksum") private String chksum;
    @JsonProperty("data")   private Gstr2bData data;

    @Data
    public static class Gstr2bData {
        @JsonProperty("gstin")   private String gstin;
        @JsonProperty("rtnprd")  private String rtnprd;
        @JsonProperty("version") private String version;
        @JsonProperty("gendt")   private String gendt;
        @JsonProperty("itcsumm") private ItcSumm itcsumm;
    }

    @Data
    public static class ItcSumm {
        @JsonProperty("itcavl")      private CategoryGroup itcavl;
        @JsonProperty("itcunavl")    private CategoryGroup itcunavl;
        @JsonProperty("itcrev")      private CategoryGroup itcrev;
        @JsonProperty("itcRejected") private CategoryGroup itcRejected;
    }

    /** One bucket (itcavl / itcunavl / itcrev / itcRejected) -> its 5 categories. */
    @Data
    public static class CategoryGroup {
        @JsonProperty("nonrevsup") private TaxBlock nonrevsup;
        @JsonProperty("isdsup")    private TaxBlock isdsup;
        @JsonProperty("revsup")    private TaxBlock revsup;
        @JsonProperty("imports")   private TaxBlock imports;
        @JsonProperty("othersup")  private TaxBlock othersup;
    }

    /** igst/cgst/sgst/cess totals for a category, plus its named sub-categories
     *  (b2b, b2ba, cdnr, cdnra, ecom, ecoma, isd, isda, impg, impgsez, impga,
     *  impgasez, cdnrrev, cdnrarev - whichever the category actually carries),
     *  captured generically via @JsonAnySetter rather than one field per name. */
    @Data
    public static class TaxBlock {
        @JsonProperty("igst") private BigDecimal igst;
        @JsonProperty("cgst") private BigDecimal cgst;
        @JsonProperty("sgst") private BigDecimal sgst;
        @JsonProperty("cess") private BigDecimal cess;

        private final Map<String, TaxBlock> subCategories = new HashMap<>();

        @JsonAnySetter
        public void setSubCategory(String name, JsonNode node) {
            if (node == null || !node.isObject()) return;
            TaxBlock sub = new TaxBlock();
            sub.setIgst(numOrNull(node.get("igst")));
            sub.setCgst(numOrNull(node.get("cgst")));
            sub.setSgst(numOrNull(node.get("sgst")));
            sub.setCess(numOrNull(node.get("cess")));
            subCategories.put(name, sub);
        }

        private BigDecimal numOrNull(JsonNode n) {
            return (n == null || n.isNull()) ? null : n.decimalValue();
        }
    }
}