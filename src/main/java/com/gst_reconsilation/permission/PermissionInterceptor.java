package com.gst_reconsilation.permission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gst_reconsilation.config.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;

/**
 * Enforces role permissions on the GST module APIs. Each path prefix maps to the
 * pages (as named in the frontend's permission matrix) whose screens use it; the
 * request passes if the caller's role grants the HTTP method's action on any of
 * them. Several pages share an API — GSTR-1 reconciles against e-invoices and
 * e-way bills, GSTR-2B and GSTR-3B read the purchase register — so a prefix can
 * list more than one page.
 *
 * The GST is taken from a companyGstId path variable or request parameter when
 * the endpoint has one, else from the X-Company-Gst-Id header the frontend sends
 * on every request. Endpoints addressed only by filingId rely on the header.
 *
 * Management APIs (roles, role-mapping, users, user-gst-mapping) are not listed
 * here: their services check against the GST of the record being changed, which
 * a client-supplied header cannot spoof.
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    public static final String GST_HEADER = "X-Company-Gst-Id";

    private record Rule(String prefix, List<String> pages) {}

    // Order matters: /api/gstr2b must be checked before /api/gstr2.
    private static final List<Rule> RULES = List.of(
            new Rule("/api/einvoice", List.of("E-Invoice", "GSTR-1")),
            new Rule("/api/ewaybill", List.of("E-Way Bill", "GSTR-1")),
            new Rule("/api/gstr1", List.of("GSTR-1", "GSTR-1A")),
            new Rule("/api/gstr2b", List.of("GSTR-2B")),
            new Rule("/api/gstr2", List.of("Inward Supply", "GSTR-2B", "GSTR-3B")),
            new Rule("/api/gstr3b", List.of("GSTR-3B")),
            new Rule("/api/ims", List.of("IMS"))
    );

    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        Rule rule = RULES.stream()
                .filter(r -> path.equals(r.prefix()) || path.startsWith(r.prefix() + "/"))
                .findFirst()
                .orElse(null);
        if (rule == null) return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = auth != null && auth.getPrincipal() instanceof Integer id ? id : null;
        if (userId == null) return true; // Spring Security already rejects unauthenticated calls

        Integer companyGstId = resolveCompanyGstId(request);
        if (companyGstId == null) {
            return deny(response, "No GSTIN selected for this request");
        }

        PermissionAction action = PermissionAction.fromHttpMethod(request.getMethod());
        if (permissionService.hasPageAccess(userId, companyGstId, rule.pages(), action)) {
            return true;
        }
        return deny(response, "Your role does not allow " + action.name().toLowerCase()
                + " on " + String.join(" / ", rule.pages()));
    }

    @SuppressWarnings("unchecked")
    private Integer resolveCompanyGstId(HttpServletRequest request) {
        Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars instanceof Map<?, ?> map) {
            Integer fromPath = parse(((Map<String, String>) map).get("companyGstId"));
            if (fromPath != null) return fromPath;
        }
        Integer fromParam = parse(request.getParameter("companyGstId"));
        if (fromParam != null) return fromParam;
        return parse(request.getHeader(GST_HEADER));
    }

    private static Integer parse(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean deny(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
        return false;
    }
}
