// ims/repository/ImsFilingRepository.java
package com.gst_reconsilation.ims.repository;

import com.gst_reconsilation.ims.entity.ImsFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ImsFilingRepository extends JpaRepository<ImsFiling, Integer> {
    Optional<ImsFiling> findByCompanyGST_IdAndRetPeriod(Integer companyGstId, String retPeriod);
}