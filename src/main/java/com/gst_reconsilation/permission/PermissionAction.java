package com.gst_reconsilation.permission;

/** The four flags a RoleMapping row grants on a screen. */
public enum PermissionAction {
    VIEW, ADD, EDIT, DELETE;

    /** GET reads, POST creates/triggers, PUT/PATCH edits, DELETE deletes. */
    public static PermissionAction fromHttpMethod(String method) {
        return switch (method.toUpperCase()) {
            case "POST" -> ADD;
            case "PUT", "PATCH" -> EDIT;
            case "DELETE" -> DELETE;
            default -> VIEW;
        };
    }
}
