package com.gst_reconsilation.roles.service;

import com.gst_reconsilation.roles.dto.RolesRequest;
import com.gst_reconsilation.roles.dto.RolesResponse;
import com.gst_reconsilation.roles.entity.Roles;
import com.gst_reconsilation.roles.repository.RolesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolesService {

    private final RolesRepository repository;

    public RolesResponse create(RolesRequest req, Integer userId) {
        Roles role = Roles.builder()
                .roleName(req.getRoleName())
                .description(req.getDescription())
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

    public RolesResponse update(Integer id, RolesRequest req, Integer userId) {
        Roles role = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));
        role.setRoleName(req.getRoleName());
        role.setDescription(req.getDescription());
        role.setUpdatedBy(userId);
        role.setUpdatedDate(LocalDate.now());
        return toResponse(repository.save(role));
    }

    public void deactivate(Integer id, Integer userId) {
        Roles role = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));
        role.setIsActive(false);
        role.setUpdatedBy(userId);
        role.setUpdatedDate(LocalDate.now());
        repository.save(role);
    }

    private RolesResponse toResponse(Roles r) {
        RolesResponse res = new RolesResponse();
        res.setId(r.getId());
        res.setRoleName(r.getRoleName());
        res.setDescription(r.getDescription());
        res.setIsActive(r.getIsActive());
        return res;
    }
}
