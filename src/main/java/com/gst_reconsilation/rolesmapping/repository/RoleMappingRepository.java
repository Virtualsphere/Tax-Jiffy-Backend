package com.gst_reconsilation.rolesmapping.repository;

import com.gst_reconsilation.rolesmapping.entity.RoleMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoleMappingRepository extends JpaRepository<RoleMapping, Integer> {
    List<RoleMapping> findByRole_IdAndCompanyGST_Id(Integer roleId, Integer companyGstId);
    List<RoleMapping> findByCompanyGST_Id(Integer companyGstId);
    List<RoleMapping> findByCompany_Id(Integer companyId);
    boolean existsByRole_IdAndCompanyGST_IdAndPageNumber(Integer roleId, Integer companyGstId, String pageNumber);
}