package com.gst_reconsilation.rolesmapping.service;

import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.entity.CompanyProfile;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.company.repository.CompanyProfileRepository;
import com.gst_reconsilation.roles.entity.Roles;
import com.gst_reconsilation.roles.repository.RolesRepository;
import com.gst_reconsilation.rolesmapping.dto.RoleMappingRequest;
import com.gst_reconsilation.rolesmapping.dto.RoleMappingResponse;
import com.gst_reconsilation.rolesmapping.entity.RoleMapping;
import com.gst_reconsilation.rolesmapping.repository.RoleMappingRepository;
import com.gst_reconsilation.permission.PermissionAction;
import com.gst_reconsilation.permission.PermissionService;
import com.gst_reconsilation.permission.dto.ScreenPermissionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleMappingService {

    private final RoleMappingRepository roleMappingRepository;
    private final RolesRepository rolesRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyGSTRepository companyGSTRepository;
    private final PermissionService permissionService;

    public RoleMappingResponse create(RoleMappingRequest req, Integer userId) {
        // Granting a screen on a new role is part of adding it; on an existing role, of editing it.
        permissionService.assertPageAccess(userId, req.getCompanyGstId(), PermissionService.PAGE_ROLE_EDITOR,
                PermissionAction.ADD, PermissionAction.EDIT);

        // One row per role + GST + page + screen (a page has several screens)
        if (roleMappingRepository.existsByRole_IdAndCompanyGST_IdAndPageNumberAndScreenNumber(
                req.getRoleId(), req.getCompanyGstId(), req.getPageNumber(), req.getScreenNumber())) {
            throw new RuntimeException("Role mapping already exists for this screen on this GST");
        }

        Roles role = rolesRepository.findById(req.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found: " + req.getRoleId()));
        assertRoleBelongsToGST(role, req.getCompanyGstId());

        CompanyProfile company = companyProfileRepository.findById(req.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found: " + req.getCompanyId()));

        CompanyGST companyGST = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));

        RoleMapping mapping = RoleMapping.builder()
                .role(role)
                .company(company)
                .companyGST(companyGST)
                .pageNumber(req.getPageNumber())
                .screenNumber(req.getScreenNumber())
                .add(req.getAdd() != null ? req.getAdd() : true)
                .edit(req.getEdit() != null ? req.getEdit() : true)
                .view(req.getView() != null ? req.getView() : true)
                .delete(req.getDelete() != null ? req.getDelete() : true)
                .createdBy(userId)
                .build();

        return toResponse(roleMappingRepository.save(mapping));
    }

    public RoleMappingResponse getById(Integer id) {
        return roleMappingRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("RoleMapping not found: " + id));
    }

    public List<RoleMappingResponse> getByCompanyGST(Integer companyGstId) {
        return roleMappingRepository.findByCompanyGST_Id(companyGstId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<RoleMappingResponse> getByRoleAndGST(Integer roleId, Integer companyGstId) {
        return roleMappingRepository.findByRole_IdAndCompanyGST_Id(roleId, companyGstId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<RoleMappingResponse> getByCompany(Integer companyId) {
        return roleMappingRepository.findByCompany_Id(companyId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RoleMappingResponse update(Integer id, RoleMappingRequest req, Integer userId) {
        RoleMapping mapping = roleMappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RoleMapping not found: " + id));
        permissionService.assertPageAccess(userId, mapping.getCompanyGST().getId(), PermissionService.PAGE_ROLE_EDITOR,
                PermissionAction.EDIT);

        // Only permissions are updatable, not role/company/gst/page
        if (req.getAdd() != null) mapping.setAdd(req.getAdd());
        if (req.getEdit() != null) mapping.setEdit(req.getEdit());
        if (req.getView() != null) mapping.setView(req.getView());
        if (req.getDelete() != null) mapping.setDelete(req.getDelete());
        if (req.getScreenNumber() != null) mapping.setScreenNumber(req.getScreenNumber());

        mapping.setUpdatedBy(userId);
        mapping.setUpdatedDate(LocalDate.now());

        return toResponse(roleMappingRepository.save(mapping));
    }

    public void delete(Integer id, Integer userId) {
        RoleMapping mapping = roleMappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RoleMapping not found: " + id));
        permissionService.assertPageAccess(userId, mapping.getCompanyGST().getId(), PermissionService.PAGE_ROLE_EDITOR,
                PermissionAction.EDIT);
        mapping.setUpdatedBy(userId);
        mapping.setUpdatedDate(LocalDate.now());
        roleMappingRepository.delete(mapping);
    }

    /**
     * Replaces a role's whole permission matrix in one transaction: rows with any
     * flag set are upserted, rows with none (or missing from the request) are
     * removed. The Add/Edit Role dialogs save through this instead of one request
     * per screen, so a failure cannot leave a role half-configured.
     */
    @Transactional
    public List<RoleMappingResponse> replaceForRole(Integer roleId, List<ScreenPermissionDto> permissions, Integer userId) {
        Roles role = rolesRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
        if (role.getCompanyGST() == null) {
            throw new RuntimeException("System roles have no editable permissions");
        }
        CompanyGST companyGST = role.getCompanyGST();
        permissionService.assertPageAccess(userId, companyGST.getId(), PermissionService.PAGE_ROLE_EDITOR,
                PermissionAction.ADD, PermissionAction.EDIT);

        Map<String, RoleMapping> existing = new HashMap<>();
        for (RoleMapping m : roleMappingRepository.findByRole_IdAndCompanyGST_Id(roleId, companyGST.getId())) {
            existing.put(key(m.getPageNumber(), m.getScreenNumber()), m);
        }

        List<RoleMapping> toSave = new ArrayList<>();
        for (ScreenPermissionDto p : permissions == null ? List.<ScreenPermissionDto>of() : permissions) {
            boolean view = Boolean.TRUE.equals(p.getView());
            boolean add = Boolean.TRUE.equals(p.getAdd());
            boolean edit = Boolean.TRUE.equals(p.getEdit());
            boolean delete = Boolean.TRUE.equals(p.getDelete());
            if (!(view || add || edit || delete)) continue; // stays in `existing`, removed below

            RoleMapping m = existing.remove(key(p.getPageNumber(), p.getScreenNumber()));
            if (m == null) {
                m = RoleMapping.builder()
                        .role(role)
                        .company(role.getCompany())
                        .companyGST(companyGST)
                        .pageNumber(p.getPageNumber())
                        .screenNumber(p.getScreenNumber())
                        .createdBy(userId)
                        .build();
            } else {
                m.setUpdatedBy(userId);
                m.setUpdatedDate(LocalDate.now());
            }
            m.setView(view);
            m.setAdd(add);
            m.setEdit(edit);
            m.setDelete(delete);
            toSave.add(m);
        }

        roleMappingRepository.deleteAll(existing.values());
        return roleMappingRepository.saveAll(toSave).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private static String key(String page, String screen) {
        return page + "||" + screen;
    }

    private static void assertRoleBelongsToGST(Roles role, Integer companyGstId) {
        if (role.getCompanyGST() == null || !role.getCompanyGST().getId().equals(companyGstId)) {
            throw new RuntimeException("Role does not belong to this GST");
        }
    }

    private RoleMappingResponse toResponse(RoleMapping m) {
        RoleMappingResponse r = new RoleMappingResponse();
        r.setId(m.getId());
        r.setPageNumber(m.getPageNumber());
        r.setScreenNumber(m.getScreenNumber());
        r.setAdd(m.getAdd());
        r.setEdit(m.getEdit());
        r.setView(m.getView());
        r.setDelete(m.getDelete());
        r.setCreatedDate(m.getCreatedDate());
        if (m.getRole() != null) {
            r.setRoleId(m.getRole().getId());
            r.setRoleName(m.getRole().getRoleName());
        }
        if (m.getCompany() != null) {
            r.setCompanyId(m.getCompany().getId());
            r.setCompanyName(m.getCompany().getCompanyName());
        }
        if (m.getCompanyGST() != null) {
            r.setCompanyGstId(m.getCompanyGST().getId());
            r.setGstNumber(m.getCompanyGST().getGstNumber());
        }
        return r;
    }
}