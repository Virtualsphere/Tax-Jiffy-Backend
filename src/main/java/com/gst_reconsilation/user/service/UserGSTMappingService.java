package com.gst_reconsilation.user.service;

import com.gst_reconsilation.user.dto.UserGSTMappingRequest;
import com.gst_reconsilation.user.dto.UserGSTMappingResponse;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.roles.entity.Roles;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.user.entity.UserGSTMapping;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.roles.repository.RolesRepository;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import com.gst_reconsilation.user.repository.UserGSTMappingRepository;
import com.gst_reconsilation.permission.PermissionAction;
import com.gst_reconsilation.permission.PermissionDeniedException;
import com.gst_reconsilation.permission.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserGSTMappingService {

    private final UserGSTMappingRepository mappingRepository;
    private final UserDetailsRepository userRepository;
    private final CompanyGSTRepository companyGSTRepository;
    private final RolesRepository rolesRepository;
    private final PermissionService permissionService;

    public UserGSTMappingResponse create(UserGSTMappingRequest req, Integer createdBy) {
        permissionService.assertPageAccess(createdBy, req.getCompanyGstId(), PermissionService.PAGE_USER_MANAGEMENT, PermissionAction.ADD);
        // Only someone with full access on the GST may hand out admin rights on it.
        if (Boolean.TRUE.equals(req.getIsAdmin()) && !permissionService.hasFullAccess(createdBy, req.getCompanyGstId())) {
            throw new PermissionDeniedException("Only a GST admin can grant admin access");
        }
        if (mappingRepository.findByUser_IdAndCompanyGST_IdAndIsActiveTrue(req.getUserId(), req.getCompanyGstId()).isPresent()) {
            throw new RuntimeException("Mapping already exists for this user and GST number");
        }

        UserDetails user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + req.getUserId()));
        CompanyGST companyGST = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));
        Roles role = rolesRepository.findById(req.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found: " + req.getRoleId()));
        assertAssignableRole(role, req.getCompanyGstId());

        UserGSTMapping mapping = UserGSTMapping.builder()
                .user(user)
                .companyGST(companyGST)
                .role(role)
                .isAdmin(req.getIsAdmin() != null && req.getIsAdmin())
                .createdBy(createdBy)
                .build();

        return toResponse(mappingRepository.save(mapping));
    }

    public List<UserGSTMappingResponse> getByUser(Integer userId) {
        return mappingRepository.findByUser_IdAndIsActiveTrue(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<UserGSTMappingResponse> getByCompanyGST(Integer companyGstId) {
        return mappingRepository.findByCompanyGST_IdAndIsActiveTrue(companyGstId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public void deactivate(Integer id, Integer updatedBy) {
        UserGSTMapping mapping = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mapping not found: " + id));
        Integer gstId = mapping.getCompanyGST().getId();
        permissionService.assertPageAccess(updatedBy, gstId, PermissionService.PAGE_USER_MANAGEMENT, PermissionAction.DELETE);
        if (Boolean.TRUE.equals(mapping.getIsAdmin()) && !permissionService.hasFullAccess(updatedBy, gstId)) {
            throw new PermissionDeniedException("Only a GST admin can remove another admin");
        }
        mapping.setIsActive(false);
        mapping.setUpdatedBy(updatedBy);
        mapping.setUpdatedDate(LocalDate.now());
        mappingRepository.save(mapping);
    }

    /** Changes which role a user has on a GST. The GST admin's own mapping stays ADMIN. */
    public UserGSTMappingResponse updateRole(Integer id, Integer roleId, Integer updatedBy) {
        UserGSTMapping mapping = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mapping not found: " + id));
        Integer gstId = mapping.getCompanyGST().getId();
        permissionService.assertPageAccess(updatedBy, gstId, PermissionService.PAGE_USER_MANAGEMENT, PermissionAction.EDIT);
        if (Boolean.TRUE.equals(mapping.getIsAdmin())) {
            throw new RuntimeException("The GST admin's role cannot be changed");
        }
        if (mapping.getUser().getId().equals(updatedBy)) {
            throw new PermissionDeniedException("You cannot change your own role");
        }

        Roles role = rolesRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
        assertAssignableRole(role, gstId);

        mapping.setRole(role);
        mapping.setUpdatedBy(updatedBy);
        mapping.setUpdatedDate(LocalDate.now());
        return toResponse(mappingRepository.save(mapping));
    }

    /** A user can only be given an active, non-ADMIN role that belongs to the same GST. */
    private void assertAssignableRole(Roles role, Integer companyGstId) {
        if (!Boolean.TRUE.equals(role.getIsActive())
                || role.getCompanyGST() == null
                || !role.getCompanyGST().getId().equals(companyGstId)) {
            throw new RuntimeException("Role does not belong to this GST");
        }
        if ("ADMIN".equalsIgnoreCase(role.getRoleName())) {
            throw new RuntimeException("The ADMIN role cannot be assigned to users");
        }
    }

    private UserGSTMappingResponse toResponse(UserGSTMapping m) {
        UserGSTMappingResponse r = new UserGSTMappingResponse();
        r.setId(m.getId());
        r.setIsAdmin(m.getIsAdmin());
        r.setIsActive(m.getIsActive());
        if (m.getUser() != null) {
            r.setUserId(m.getUser().getId());
            r.setUserName(m.getUser().getUserName());
        }
        if (m.getCompanyGST() != null) {
            r.setCompanyGstId(m.getCompanyGST().getId());
            r.setGstNumber(m.getCompanyGST().getGstNumber());
        }
        if (m.getRole() != null) {
            r.setRoleId(m.getRole().getId());
            r.setRoleName(m.getRole().getRoleName());
        }
        return r;
    }
}