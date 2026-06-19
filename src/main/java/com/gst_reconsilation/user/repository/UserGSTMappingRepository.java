package com.gst_reconsilation.user.repository;

import com.gst_reconsilation.user.entity.UserGSTMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserGSTMappingRepository extends JpaRepository<UserGSTMapping, Integer> {
    List<UserGSTMapping> findByUser_IdAndIsActiveTrue(Integer userId);
    List<UserGSTMapping> findByCompanyGST_IdAndIsActiveTrue(Integer companyGstId);
    Optional<UserGSTMapping> findByUser_IdAndCompanyGST_IdAndIsActiveTrue(Integer userId, Integer companyGstId);
    List<UserGSTMapping> findByUser_IdAndIsActiveTrueAndIsAdminTrue(Integer userId);
}