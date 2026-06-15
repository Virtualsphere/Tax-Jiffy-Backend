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

    public UserGSTMappingResponse create(UserGSTMappingRequest req, Integer createdBy) {
        if (mappingRepository.findByUser_IdAndCompanyGST_IdAndIsActiveTrue(req.getUserId(), req.getCompanyGstId()).isPresent()) {
            throw new RuntimeException("Mapping already exists for this user and GST number");
        }

        UserDetails user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + req.getUserId()));
        CompanyGST companyGST = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));
        Roles role = rolesRepository.findById(req.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found: " + req.getRoleId()));

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
        mapping.setIsActive(false);
        mapping.setUpdatedBy(updatedBy);
        mapping.setUpdatedDate(LocalDate.now());
        mappingRepository.save(mapping);
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
            r.setRoleName(m.getRole().getRoleName());
        }
        return r;
    }
}