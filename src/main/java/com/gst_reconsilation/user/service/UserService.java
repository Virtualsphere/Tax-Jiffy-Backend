package com.gst_reconsilation.user.service;

import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.user.dto.UserRequest;
import com.gst_reconsilation.user.dto.UserResponse;
import com.gst_reconsilation.company.entity.CompanyProfile;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.company.repository.CompanyProfileRepository;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import com.gst_reconsilation.user.repository.UserGSTMappingRepository;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.permission.PermissionAction;
import com.gst_reconsilation.permission.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.gst_reconsilation.roles.entity.Roles;
import com.gst_reconsilation.user.entity.UserGSTMapping;
import com.gst_reconsilation.roles.repository.RolesRepository;
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDetailsRepository userRepository;
    private final CompanyProfileRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserGSTMappingRepository userGSTMappingRepository;
    private final CompanyGSTRepository companyGSTRepository;
    private final RolesRepository rolesRepository;
    private final PermissionService permissionService;

    public UserResponse create(UserRequest req, Integer createdBy) {
        permissionService.assertPageAccess(createdBy, req.getCompanyGstId(), PermissionService.PAGE_USER_MANAGEMENT, PermissionAction.ADD);

        if (userRepository.existsByUserEmail(req.getUserEmail())) {
            throw new RuntimeException("Email already registered: " + req.getUserEmail());
        }

        CompanyGST gst = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));

        if (!Boolean.TRUE.equals(gst.getIsPaymentDone()) || gst.getSubscriptionPlan() == null) {
            throw new RuntimeException("No active subscription for this GST number");
        }

        long currentCount = userGSTMappingRepository.countByCompanyGST_IdAndIsActiveTrue(req.getCompanyGstId());
        int allowedCount = gst.getSubscriptionPlan().getUserCount();
        if (currentCount >= allowedCount) {
            throw new RuntimeException("User limit reached (" + allowedCount + " users allowed on this GST's plan)");
        }

        Roles userRole = req.getRoleId() != null
                ? assignableRole(req.getRoleId(), gst.getId())
                // Scoped USER role for this company + GST — created on first use instead
                // of relying on a globally-seeded role.
                : rolesRepository.findByRoleNameAndCompanyGST_IdAndIsActiveTrue("USER", gst.getId())
                        .orElseGet(() -> rolesRepository.save(Roles.builder()
                                .roleName("USER")
                                .description("User role for GST " + gst.getGstNumber())
                                .company(gst.getCompany())
                                .companyGST(gst)
                                .createdBy(createdBy)
                                .build()));

        UserDetails user = UserDetails.builder()
                .company(gst.getCompany())
                .role(userRole)
                .userName(req.getUserName())
                .userEmail(req.getUserEmail())
                .userPassword(passwordEncoder.encode(req.getUserPassword()))
                .mobile(req.getMobile())
                .createdBy(createdBy)
                .build();
        user = userRepository.save(user);

        UserGSTMapping mapping = UserGSTMapping.builder()
                .user(user)
                .companyGST(gst)
                .role(userRole)
                .isAdmin(false)
                .createdBy(createdBy)
                .build();
        userGSTMappingRepository.save(mapping);

        return toResponse(user);
    }

    public UserResponse register(UserRequest req) {
        if (userRepository.existsByUserEmail(req.getUserEmail())) {
            throw new RuntimeException("Email already registered: " + req.getUserEmail());
        }
        UserDetails user = UserDetails.builder()
                .userName(req.getUserName())
                .userEmail(req.getUserEmail())
                .userPassword(passwordEncoder.encode(req.getUserPassword()))
                .mobile(req.getMobile())
                .build();
        return toResponse(userRepository.save(user));
    }

    public UserResponse getById(Integer id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public List<UserResponse> getByCompany(Integer companyId) {
        return userRepository.findByCompany_IdAndIsActiveTrue(companyId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public UserResponse update(Integer id, UserRequest req, Integer updatedBy) {
        UserDetails user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));


        user.setUserName(req.getUserName());
        if (req.getMobile() != null) user.setMobile(req.getMobile());
        user.setUpdatedBy(updatedBy);
        user.setUpdatedDate(LocalDate.now());
        return toResponse(userRepository.save(user));
    }

    public void deactivate(Integer id, Integer updatedBy) {
        UserDetails user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setIsActive(false);
        user.setUpdatedBy(updatedBy);
        user.setUpdatedDate(LocalDate.now());
        userRepository.save(user);
    }

    private UserResponse toResponse(UserDetails u) {
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        r.setUserName(u.getUserName());
        r.setUserEmail(u.getUserEmail());
        r.setMobile(u.getMobile());
        r.setIsActive(u.getIsActive());
        if (u.getCompany() != null) {
            r.setCompanyId(u.getCompany().getId());
            r.setCompanyName(u.getCompany().getCompanyName());
        }
        if (u.getRole() != null) {
            r.setRoleName(u.getRole().getRoleName());
        }
        return r;
    }

    private Roles assignableRole(Integer roleId, Integer companyGstId) {
        Roles role = rolesRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
        if (!Boolean.TRUE.equals(role.getIsActive())
                || role.getCompanyGST() == null
                || !role.getCompanyGST().getId().equals(companyGstId)) {
            throw new RuntimeException("Role does not belong to this GST");
        }
        if ("ADMIN".equalsIgnoreCase(role.getRoleName())) {
            throw new RuntimeException("The ADMIN role cannot be assigned to users");
        }
        return role;
    }
}