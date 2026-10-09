package com.gst_reconsilation.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One row of the permission matrix: page + screen and its four flags. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreenPermissionDto {
    private String pageNumber;
    private String screenNumber;
    private Boolean view;
    private Boolean add;
    private Boolean edit;
    private Boolean delete;
}
