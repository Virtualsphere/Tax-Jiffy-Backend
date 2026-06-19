package com.gst_reconsilation.company.repository;

import com.gst_reconsilation.company.entity.CompanyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Integer> {
    List<CompanyProfile> findByIsActiveTrue();
    List<CompanyProfile> findByOwnerUserIdAndIsActiveTrue(Integer ownerUserId);
}