package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr2Filing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface Gstr2FilingRepository extends JpaRepository<Gstr2Filing, Integer> {
    List<Gstr2Filing> findByCompanyGST_IdAndIsActiveTrue(Integer companyGstId);
    Optional<Gstr2Filing> findByCompanyGST_IdAndFinancialYearAndTaxPeriod(
            Integer companyGstId, String financialYear, String taxPeriod);
    List<Gstr2Filing> findByCreatedByAndIsActiveTrue(Integer userId);
}