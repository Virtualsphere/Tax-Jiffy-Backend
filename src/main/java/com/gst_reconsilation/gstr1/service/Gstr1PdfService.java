package com.gst_reconsilation.gstr1.service;

import com.gst_reconsilation.gstr1.dto.report.Gstr1ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class Gstr1PdfService {

    private final Gstr1ReportService reportService;
    private final TemplateEngine templateEngine;

    private volatile Path notoSansFontPath;

    public byte[] generatePdf(Integer filingId) {
        Gstr1ReportResponse report = reportService.buildReport(filingId);

        Context ctx = new Context();
        ctx.setVariable("report", report);
        String html = templateEngine.process("pdf/gstr1-report", ctx);

        try {
            ITextRenderer renderer = new ITextRenderer();

            // Embed a Unicode font so the ₹ symbol and Indian names render correctly —
            // OpenPDF's built-in fonts don't cover ₹ (U+20B9) and will show a blank box.
            renderer.getFontResolver().addFont(
                    resolveNotoSansFontPath(),
                    "identity-H",
                    true);

            renderer.setDocumentFromString(html);
            renderer.layout();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate GSTR-1 PDF for filing " + filingId, e);
        }
    }

    /**
     * ITextRenderer's font resolver needs a real filesystem path, not raw bytes.
     * Since the font ships on the classpath (and may be inside a jar), copy it out
     * to a temp file once and reuse that path on subsequent calls.
     */
    private String resolveNotoSansFontPath() throws Exception {
        if (notoSansFontPath == null) {
            synchronized (this) {
                if (notoSansFontPath == null) {
                    Path tempFile = Files.createTempFile("NotoSans-Regular", ".ttf");
                    tempFile.toFile().deleteOnExit();
                    try (InputStream fontStream =
                                 new ClassPathResource("fonts/NotoSans-Regular.ttf").getInputStream()) {
                        Files.copy(fontStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    notoSansFontPath = tempFile;
                }
            }
        }
        return notoSansFontPath.toString();
    }
}