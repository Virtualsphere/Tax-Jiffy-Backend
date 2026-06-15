package com.gst_reconsilation.company.service;

import com.gst_reconsilation.company.dto.CompanyProfileRequest;
import com.gst_reconsilation.company.dto.CompanyProfileResponse;
import com.gst_reconsilation.company.entity.CompanyProfile;
import com.gst_reconsilation.company.repository.CompanyProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyProfileService {

    private final CompanyProfileRepository repository;

    public CompanyProfileResponse create(CompanyProfileRequest req, Integer userId) {
        CompanyProfile company = CompanyProfile.builder()
                .companyName(req.getCompanyName())
                .companyLogo(req.getCompanyLogo())
                .createdBy(userId)
                .build();
        return toResponse(repository.save(company));
    }

    public List<CompanyProfileResponse> getAll() {
        return repository.findByIsActiveTrue().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CompanyProfileResponse getById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Company not found: " + id));
    }

    public CompanyProfileResponse update(Integer id, CompanyProfileRequest req, Integer userId) {
        CompanyProfile company = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found: " + id));
        company.setCompanyName(req.getCompanyName());
        company.setCompanyLogo(req.getCompanyLogo());
        company.setUpdatedBy(userId);
        company.setUpdatedDate(LocalDate.now());
        return toResponse(repository.save(company));
    }

    public void deactivate(Integer id, Integer userId) {
        CompanyProfile company = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found: " + id));
        company.setIsActive(false);
        company.setUpdatedBy(userId);
        company.setUpdatedDate(LocalDate.now());
        repository.save(company);
    }

    private CompanyProfileResponse toResponse(CompanyProfile c) {
        CompanyProfileResponse r = new CompanyProfileResponse();
        r.setId(c.getId());
        r.setCompanyName(c.getCompanyName());
        r.setCompanyLogo(c.getCompanyLogo());
        r.setIsActive(c.getIsActive());
        return r;
    }
}