package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr2Cdnr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr2CdnrRepository extends JpaRepository<Gstr2Cdnr, Integer> {
    List<Gstr2Cdnr> findByFiling_Id(Integer filingId);
    List<Gstr2Cdnr> findByFiling_IdAndGstinOfSupplier(Integer filingId, String gstinOfSupplier);
    void deleteByFiling_Id(Integer filingId);
}