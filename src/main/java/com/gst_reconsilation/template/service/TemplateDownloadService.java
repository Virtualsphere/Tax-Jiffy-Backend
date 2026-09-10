package com.gst_reconsilation.template.service;

import com.gst_reconsilation.template.TemplateType;
import com.gst_reconsilation.template.dto.TemplateInfo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TemplateDownloadService {

    private static final String BASE_PATH = "excel-templates/";

    public List<TemplateInfo> listTemplates() {
        return Arrays.stream(TemplateType.values())
                .map(t -> TemplateInfo.builder()
                        .key(t.getKey())
                        .label(t.getLabel())
                        .fileName(t.getDownloadFileName())
                        .downloadUrl("/api/templates/" + t.getKey())
                        .build())
                .toList();
    }

    public Resource loadResource(TemplateType type) {
        Resource resource = new ClassPathResource(BASE_PATH + type.getClasspathFileName());
        if (!resource.exists()) {
            throw new IllegalStateException("Template file not found on server: " + type.getClasspathFileName());
        }
        return resource;
    }

    public TemplateType resolve(String key) {
        return TemplateType.fromKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Unknown template: " + key));
    }
}
