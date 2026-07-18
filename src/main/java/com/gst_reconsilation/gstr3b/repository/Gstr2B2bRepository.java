package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr2B2b;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr2B2bRepository extends JpaRepository<Gstr2B2b, Integer> {
    List<Gstr2B2b> findByFiling_Id(Integer filingId);
    List<Gstr2B2b> findByFiling_IdAndGstinOfSupplier(Integer filingId, String gstinOfSupplier);
    void deleteByFiling_Id(Integer filingId);
}