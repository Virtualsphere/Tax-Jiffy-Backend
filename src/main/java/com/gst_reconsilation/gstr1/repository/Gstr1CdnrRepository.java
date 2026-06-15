package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Cdnr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1CdnrRepository extends JpaRepository<Gstr1Cdnr, Integer> {
    List<Gstr1Cdnr> findByFiling_Id(Integer filingId);
    List<Gstr1Cdnr> findByFiling_IdAndGstinOfRecipient(Integer filingId, String gstinOfRecipient);
    void deleteByFiling_Id(Integer filingId);
}