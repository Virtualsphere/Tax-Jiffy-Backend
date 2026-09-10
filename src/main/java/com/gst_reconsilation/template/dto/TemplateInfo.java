package com.gst_reconsilation.template.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateInfo {
    private String key;
    private String label;
    private String fileName;
    private String downloadUrl;
}
