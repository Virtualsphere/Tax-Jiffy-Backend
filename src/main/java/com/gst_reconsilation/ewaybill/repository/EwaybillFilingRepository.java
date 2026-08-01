// ewaybill/repository/EwaybillFilingRepository.java
package com.gst_reconsilation.ewaybill.repository;

import com.gst_reconsilation.ewaybill.entity.EwaybillFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EwaybillFilingRepository extends JpaRepository<EwaybillFiling, Integer> {
    Optional<EwaybillFiling> findByCompanyGST_IdAndSyncDate(Integer companyGstId, String syncDate);
}