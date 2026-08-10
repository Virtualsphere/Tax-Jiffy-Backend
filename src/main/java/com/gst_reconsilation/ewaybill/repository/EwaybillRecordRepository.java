// ewaybill/repository/EwaybillRecordRepository.java
package com.gst_reconsilation.ewaybill.repository;

import com.gst_reconsilation.ewaybill.entity.EwaybillRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EwaybillRecordRepository extends JpaRepository<EwaybillRecord, Integer> {
    List<EwaybillRecord> findByFiling_Id(Integer filingId);
    Optional<EwaybillRecord> findByEwbNo(Long ewbNo);
    boolean existsByEwbNo(Long ewbNo);

    /** All e-way bills ever synced/uploaded for a company, across every sync-date batch — filtered
     *  by month in the service layer since e-way bills are stored per sync date, not per GST period. */
    List<EwaybillRecord> findByFiling_CompanyGST_Id(Integer companyGstId);
}