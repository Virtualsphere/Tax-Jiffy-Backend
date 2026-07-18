package com.gst_reconsilation.roles.repository;

import com.gst_reconsilation.roles.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RolesRepository extends JpaRepository<Roles, Integer> {
    List<Roles> findByIsActiveTrue();

    // Global/system roles only (e.g. SUPER_ADMIN) — companyGST is null.
    Optional<Roles> findByRoleNameAndCompanyGSTIsNullAndIsActiveTrue(String roleName);

    // Tenant-scoped roles — unique per (roleName, companyGstId).
    Optional<Roles> findByRoleNameAndCompanyGST_IdAndIsActiveTrue(String roleName, Integer companyGstId);

    List<Roles> findByCompanyGST_IdAndIsActiveTrue(Integer companyGstId);
    List<Roles> findByCompany_IdAndIsActiveTrue(Integer companyId);
    List<Roles> findByCompany_IdAndCompanyGST_IdAndIsActiveTrue(Integer companyId, Integer companyGstId);
}