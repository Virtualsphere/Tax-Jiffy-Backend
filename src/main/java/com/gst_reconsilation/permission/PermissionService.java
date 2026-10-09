package com.gst_reconsilation.permission;

import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.permission.dto.MyPermissionsResponse;
import com.gst_reconsilation.permission.dto.ScreenPermissionDto;
import com.gst_reconsilation.rolesmapping.entity.RoleMapping;
import com.gst_reconsilation.rolesmapping.repository.RoleMappingRepository;
import com.gst_reconsilation.user.entity.UserGSTMapping;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import com.gst_reconsilation.user.repository.UserGSTMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Resolves what a user may do on a GST from the role assigned to them in
 * UserGSTMapping and that role's RoleMapping rows.
 *
 * Super admins, the company owner, and GST admins (UserGSTMapping.isAdmin) get
 * full access without consulting RoleMapping — the ADMIN role created on
 * purchase has no mapping rows, and must not be locked out of its own GST.
 *
 * Page names are the ones the frontend's permission matrix stores in
 * RoleMapping.pageNumber (src/config/app-pages.ts), e.g. "GSTR-1", "Role Editor".
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    public static final String PAGE_ROLE_EDITOR = "Role Editor";
    public static final String PAGE_USER_MANAGEMENT = "User Management";

    private final UserDetailsRepository userDetailsRepository;
    private final UserGSTMappingRepository userGSTMappingRepository;
    private final CompanyGSTRepository companyGSTRepository;
    private final RoleMappingRepository roleMappingRepository;

    @Transactional(readOnly = true)
    public MyPermissionsResponse getMyPermissions(Integer userId, Integer companyGstId) {
        if (hasFullAccess(userId, companyGstId)) {
            Optional<UserGSTMapping> mapping = activeMapping(userId, companyGstId);
            return new MyPermissionsResponse(
                    companyGstId, true,
                    mapping.map(m -> m.getRole().getId()).orElse(null),
                    mapping.map(m -> m.getRole().getRoleName()).orElse("ADMIN"),
                    List.of());
        }

        UserGSTMapping mapping = activeMapping(userId, companyGstId)
                .orElseThrow(() -> new PermissionDeniedException("You do not have access to this GSTIN"));

        List<ScreenPermissionDto> rows = roleMappingRepository
                .findByRole_IdAndCompanyGST_Id(mapping.getRole().getId(), companyGstId)
                .stream()
                .map(m -> new ScreenPermissionDto(
                        m.getPageNumber(), m.getScreenNumber(),
                        m.getView(), m.getAdd(), m.getEdit(), m.getDelete()))
                .toList();

        return new MyPermissionsResponse(
                companyGstId, false, mapping.getRole().getId(), mapping.getRole().getRoleName(), rows);
    }

    /** True if the user may perform the action on at least one screen of any of the given pages. */
    @Transactional(readOnly = true)
    public boolean hasPageAccess(Integer userId, Integer companyGstId, Collection<String> pages, PermissionAction action) {
        if (userId == null || companyGstId == null) return false;
        if (hasFullAccess(userId, companyGstId)) return true;

        Optional<UserGSTMapping> mapping = activeMapping(userId, companyGstId);
        if (mapping.isEmpty()) return false;

        return roleMappingRepository
                .findByRole_IdAndCompanyGST_Id(mapping.get().getRole().getId(), companyGstId)
                .stream()
                .anyMatch(m -> pages.contains(m.getPageNumber()) && grants(m, action));
    }

    public void assertPageAccess(Integer userId, Integer companyGstId, String page, PermissionAction... anyOf) {
        for (PermissionAction action : anyOf) {
            if (hasPageAccess(userId, companyGstId, List.of(page), action)) return;
        }
        throw new PermissionDeniedException(
                "Your role does not allow " + anyOf[0].name().toLowerCase() + " on " + page);
    }

    /** Super admin, owner of the GST's company, or flagged admin on the GST. */
    @Transactional(readOnly = true)
    public boolean hasFullAccess(Integer userId, Integer companyGstId) {
        boolean superAdmin = userDetailsRepository.findById(userId)
                .map(u -> Boolean.TRUE.equals(u.getIsSuperAdmin()))
                .orElse(false);
        if (superAdmin) return true;

        Optional<CompanyGST> gst = companyGSTRepository.findById(companyGstId);
        if (gst.isPresent() && userId.equals(gst.get().getCompany().getOwnerUserId())) return true;

        return activeMapping(userId, companyGstId)
                .map(m -> Boolean.TRUE.equals(m.getIsAdmin()))
                .orElse(false);
    }

    private Optional<UserGSTMapping> activeMapping(Integer userId, Integer companyGstId) {
        return userGSTMappingRepository.findByUser_IdAndCompanyGST_IdAndIsActiveTrue(userId, companyGstId);
    }

    private static boolean grants(RoleMapping m, PermissionAction action) {
        return switch (action) {
            case VIEW -> Boolean.TRUE.equals(m.getView());
            case ADD -> Boolean.TRUE.equals(m.getAdd());
            case EDIT -> Boolean.TRUE.equals(m.getEdit());
            case DELETE -> Boolean.TRUE.equals(m.getDelete());
        };
    }
}
