package com.gst_reconsilation.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * What the caller may do on one GST. When fullAccess is true (super admin, company
 * owner, or GST admin) the permissions list is empty and every check passes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyPermissionsResponse {
    private Integer companyGstId;
    private Boolean fullAccess;
    private Integer roleId;
    private String roleName;
    private List<ScreenPermissionDto> permissions;
}
