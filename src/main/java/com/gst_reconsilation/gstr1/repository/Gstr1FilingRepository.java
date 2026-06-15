package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Filing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface Gstr1FilingRepository extends JpaRepository<Gstr1Filing, Integer> {
    List<Gstr1Filing> findByCompanyGST_IdAndIsActiveTrue(Integer companyGstId);
    Optional<Gstr1Filing> findByCompanyGST_IdAndFinancialYearAndTaxPeriod(
            Integer companyGstId, String financialYear, String taxPeriod);
    List<Gstr1Filing> findByCreatedByAndIsActiveTrue(Integer userId);
}