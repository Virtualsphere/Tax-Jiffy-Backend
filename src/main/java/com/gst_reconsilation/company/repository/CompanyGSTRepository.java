package com.gst_reconsilation.company.repository;

import com.gst_reconsilation.company.entity.CompanyGST;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyGSTRepository extends JpaRepository<CompanyGST, Integer> {
    Optional<CompanyGST> findByGstNumber(String gstNumber);
    List<CompanyGST> findByCompany_IdAndIsActiveTrue(Integer companyId);
    boolean existsByGstNumber(String gstNumber);
}