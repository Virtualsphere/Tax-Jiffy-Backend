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

    public UserResponse create(UserRequest req, Integer createdBy) {
        assertCallerIsAdminOfGST(createdBy, req.getCompanyGstId());

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

        Roles userRole = rolesRepository.findByRoleNameAndIsActiveTrue("USER")
                .orElseThrow(() -> new RuntimeException("USER role not seeded"));

        UserDetails user = UserDetails.builder()
                .company(gst.getCompany())   // fixed: link to the GST's company
                .role(userRole)              // fixed: record their role
                .userName(req.getUserName())
                .userEmail(req.getUserEmail())
                .userPassword(passwordEncoder.encode(req.getUserPassword()))
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

    private void assertCallerIsAdminOfGST(Integer callerId, Integer companyGstId) {
        UserDetails caller = userRepository.findById(callerId)
                .orElseThrow(() -> new RuntimeException("Caller not found"));
        if (Boolean.TRUE.equals(caller.getIsSuperAdmin())) return;

        boolean isAdmin = userGSTMappingRepository
                .findByUser_IdAndIsActiveTrueAndIsAdminTrue(callerId)
                .stream()
                .anyMatch(m -> m.getCompanyGST().getId().equals(companyGstId));

        if (!isAdmin) throw new RuntimeException("Only the GST admin can add users to this GST");
    }
}