package com.gst_reconsilation.template;

import java.util.Arrays;
import java.util.Optional;

/**
 * Registry of downloadable Excel templates. The classpathFileName must match a file
 * under src/main/resources/excel-templates/.
 */
public enum TemplateType {

    GSTR1("gstr1", "GSTR-1 Excel Workbook Template",
            "GSTR1_Excel_Workbook_Template_V2.2.xlsx", "GSTR1_Excel_Workbook_Template.xlsx"),

    GSTR2("gstr2", "GSTR-2 / Purchase Return Excel Workbook Template",
            "GSTR2_Excel_Workbook_TemplateNew_V1.1.xlsx", "GSTR2_Purchase_Return_Template.xlsx"),

    EINVOICE("einvoice", "E-Invoice Manual Entry Template",
            "EInvoice_Manual_Entry_Template_V2.xlsx", "EInvoice_Manual_Entry_Template.xlsx"),

    EWAYBILL("ewaybill", "E-Way Bill Manual Entry Template",
            "EWayBill_Manual_Entry_Template_V2.xlsx", "EWayBill_Manual_Entry_Template.xlsx"),

    IMS("ims", "IMS Upload Template",
            "ims-upload-template.xlsx", "IMS_Upload_Template.xlsx");

    private final String key;
    private final String label;
    private final String classpathFileName;
    private final String downloadFileName;

    TemplateType(String key, String label, String classpathFileName, String downloadFileName) {
        this.key = key;
        this.label = label;
        this.classpathFileName = classpathFileName;
        this.downloadFileName = downloadFileName;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getClasspathFileName() {
        return classpathFileName;
    }

    public String getDownloadFileName() {
        return downloadFileName;
    }

    public static Optional<TemplateType> fromKey(String key) {
        return Arrays.stream(values())
                .filter(t -> t.key.equalsIgnoreCase(key))
                .findFirst();
    }
}
