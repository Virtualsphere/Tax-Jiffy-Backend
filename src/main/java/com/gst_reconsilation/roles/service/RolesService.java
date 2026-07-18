package com.gst_reconsilation.roles.service;

import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.entity.CompanyProfile;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.company.repository.CompanyProfileRepository;
import com.gst_reconsilation.roles.dto.RolesRequest;
import com.gst_reconsilation.roles.dto.RolesResponse;
import com.gst_reconsilation.roles.entity.Roles;
import com.gst_reconsilation.roles.repository.RolesRepository;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import com.gst_reconsilation.user.repository.UserGSTMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolesService {

    private final RolesRepository repository;
    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyGSTRepository companyGSTRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final UserGSTMappingRepository userGSTMappingRepository;

    /**
     * A GST's own admin (or a platform super admin) creates additional roles
     * scoped to that company + GST. This is no longer super-admin-only.
     */
    public RolesResponse create(RolesRequest req, Integer userId) {
        if (req.getCompanyId() == null || req.getCompanyGstId() == null) {
            throw new RuntimeException("companyId and companyGstId are required to create a role");
        }

        assertCallerIsAdminOfGST(userId, req.getCompanyGstId());

        CompanyProfile company = companyProfileRepository.findById(req.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found: " + req.getCompanyId()));
        CompanyGST companyGST = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));

        if (!companyGST.getCompany().getId().equals(company.getId())) {
            throw new RuntimeException("companyGstId does not belong to companyId");
        }

        if (repository.findByRoleNameAndCompanyGST_IdAndIsActiveTrue(req.getRoleName(), req.getCompanyGstId()).isPresent()) {
            throw new RuntimeException("Role '" + req.getRoleName() + "' already exists for this GST");
        }

        Roles role = Roles.builder()
                .roleName(req.getRoleName())
                .description(req.getDescription())
                .company(company)
                .companyGST(companyGST)
                .createdBy(userId)
                .build();
        return toResponse(repository.save(role));
    }

    public List<RolesResponse> getAll() {
        return repository.findByIsActiveTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RolesResponse getById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));
    }

    // NEW: fetch all roles for a given company + GST number
    public List<RolesResponse> getByCompanyAndGst(Integer companyId, Integer companyGstId) {
        return repository.findByCompany_IdAndCompanyGST_IdAndIsActiveTrue(companyId, companyGstId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RolesResponse update(Integer id, RolesRequest req, Integer userId) {
        Roles role = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));

        if (role.getCompanyGST() != null) {
            assertCallerIsAdminOfGST(userId, role.getCompanyGST().getId());
        }

        role.setRoleName(req.getRoleName());
        role.setDescription(req.getDescription());
        role.setUpdatedBy(userId);
        role.setUpdatedDate(LocalDate.now());
        return toResponse(repository.save(role));
    }

    public void deactivate(Integer id, Integer userId) {
        Roles role = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));

        if (role.getCompanyGST() != null) {
            assertCallerIsAdminOfGST(userId, role.getCompanyGST().getId());
        }

        role.setIsActive(false);
        role.setUpdatedBy(userId);
        role.setUpdatedDate(LocalDate.now());
        repository.save(role);
    }

    private void assertCallerIsAdminOfGST(Integer callerId, Integer companyGstId) {
        UserDetails caller = userDetailsRepository.findById(callerId)
                .orElseThrow(() -> new RuntimeException("Caller not found"));
        if (Boolean.TRUE.equals(caller.getIsSuperAdmin())) return;

        boolean isAdmin = userGSTMappingRepository
                .findByUser_IdAndIsActiveTrueAndIsAdminTrue(callerId)
                .stream()
                .anyMatch(m -> m.getCompanyGST().getId().equals(companyGstId));

        if (!isAdmin) throw new RuntimeException("Only the GST admin can manage roles for this GST");
    }

    private RolesResponse toResponse(Roles r) {
        RolesResponse res = new RolesResponse();
        res.setId(r.getId());
        res.setRoleName(r.getRoleName());
        res.setDescription(r.getDescription());
        res.setIsActive(r.getIsActive());
        if (r.getCompany() != null) {
            res.setCompanyId(r.getCompany().getId());
            res.setCompanyName(r.getCompany().getCompanyName());
        }
        if (r.getCompanyGST() != null) {
            res.setCompanyGstId(r.getCompanyGST().getId());
            res.setGstNumber(r.getCompanyGST().getGstNumber());
        }
        return res;
    }
}