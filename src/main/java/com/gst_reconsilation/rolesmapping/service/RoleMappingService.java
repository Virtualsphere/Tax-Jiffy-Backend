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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleMappingService {

    private final RoleMappingRepository roleMappingRepository;
    private final RolesRepository rolesRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyGSTRepository companyGSTRepository;

    public RoleMappingResponse create(RoleMappingRequest req, Integer userId) {
        // Prevent duplicate page mapping for same role + GST
        if (roleMappingRepository.existsByRole_IdAndCompanyGST_IdAndPageNumber(
                req.getRoleId(), req.getCompanyGstId(), req.getPageNumber())) {
            throw new RuntimeException("Role mapping already exists for this page on this GST");
        }

        Roles role = rolesRepository.findById(req.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found: " + req.getRoleId()));

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
        mapping.setUpdatedBy(userId);
        mapping.setUpdatedDate(LocalDate.now());
        roleMappingRepository.delete(mapping);
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