package com.gst_reconsilation.template.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.template.TemplateType;
import com.gst_reconsilation.template.dto.TemplateInfo;
import com.gst_reconsilation.template.service.TemplateDownloadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Templates", description = "Download blank Excel templates for GSTR-1, GSTR-2 (purchase return), e-Invoice, e-Way Bill and IMS uploads")
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateDownloadController {

    private final TemplateDownloadService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateInfo>>> list() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listTemplates()));
    }

    @GetMapping("/gstr1")
    public ResponseEntity<Resource> downloadGstr1() {
        return download(TemplateType.GSTR1);
    }

    @GetMapping("/gstr2")
    public ResponseEntity<Resource> downloadGstr2() {
        return download(TemplateType.GSTR2);
    }

    @GetMapping("/einvoice")
    public ResponseEntity<Resource> downloadEinvoice() {
        return download(TemplateType.EINVOICE);
    }

    @GetMapping("/ewaybill")
    public ResponseEntity<Resource> downloadEwaybill() {
        return download(TemplateType.EWAYBILL);
    }

    @GetMapping("/ims")
    public ResponseEntity<Resource> downloadIms() {
        return download(TemplateType.IMS);
    }

    @GetMapping("/{key}")
    public ResponseEntity<Resource> downloadByKey(@PathVariable String key) {
        return download(service.resolve(key));
    }

    private ResponseEntity<Resource> download(TemplateType type) {
        Resource resource = service.loadResource(type);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + type.getDownloadFileName() + "\"")
                .body(resource);
    }
}
