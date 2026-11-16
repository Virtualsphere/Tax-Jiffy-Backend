package com.gst_reconsilation.whitebooks;

import lombok.Getter;

/**
 * Whitebooks / GSTN rejected a call. Whitebooks reports most failures as HTTP 200 with
 * {"status_cd":"0", "status_desc": "...", "error": {"errorCode": "...", "errorMessage": "..."}},
 * so this is raised by {@link WhitebooksClient} whenever status_cd is not "1".
 */
@Getter
public class WhitebooksApiException extends RuntimeException {

    private final String errorCode;

    private static final java.util.regex.Pattern NO_DATA = java.util.regex.Pattern.compile(
            "(?i)no (invoices?|records?|data)( found)?|data not found|not filed");

    public WhitebooksApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * GSTN reports an empty section/period as an error ("No Invoices found ...").
     * That is a normal empty result, not a failure. The wording is matched loosely because
     * GSTN's exact texts vary; extend this if real responses use other phrasing.
     */
    public static boolean isNoData(Throwable e) {
        return e instanceof WhitebooksApiException && e.getMessage() != null
                && NO_DATA.matcher(e.getMessage()).find();
    }
}
