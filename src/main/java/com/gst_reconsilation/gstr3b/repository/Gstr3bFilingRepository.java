package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr3bFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface Gstr3bFilingRepository extends JpaRepository<Gstr3bFiling, Integer> {
    List<Gstr3bFiling> findByCompanyGST_IdAndIsActiveTrue(Integer companyGstId);
    Optional<Gstr3bFiling> findByCompanyGST_IdAndFinancialYearAndTaxPeriod(
            Integer companyGstId, String financialYear, String taxPeriod);
}