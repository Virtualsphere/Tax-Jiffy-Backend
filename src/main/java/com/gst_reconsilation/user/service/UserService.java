package com.gst_reconsilation.user.service;

import com.gst_reconsilation.user.dto.UserRequest;
import com.gst_reconsilation.user.dto.UserResponse;
import com.gst_reconsilation.company.entity.CompanyProfile;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.company.repository.CompanyProfileRepository;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDetailsRepository userRepository;
    private final CompanyProfileRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse create(UserRequest req, Integer createdBy) {
        if (userRepository.existsByUserEmail(req.getUserEmail())) {
            throw new RuntimeException("Email already registered: " + req.getUserEmail());
        }
        CompanyProfile company = null;
        if (req.getCompanyId() != null) {
            company = companyRepository.findById(req.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found: " + req.getCompanyId()));
        }
        UserDetails user = UserDetails.builder()
                .company(company)
                .userName(req.getUserName())
                .userEmail(req.getUserEmail())
                .userPassword(passwordEncoder.encode(req.getUserPassword()))
                .createdBy(createdBy)
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
        if (req.getCompanyId() != null) {
            CompanyProfile company = companyRepository.findById(req.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found: " + req.getCompanyId()));
            user.setCompany(company);
        }
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
        return r;
    }
}