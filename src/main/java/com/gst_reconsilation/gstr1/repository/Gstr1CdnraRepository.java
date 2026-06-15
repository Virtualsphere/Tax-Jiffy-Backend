package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Cdnra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1CdnraRepository extends JpaRepository<Gstr1Cdnra, Integer> {
    List<Gstr1Cdnra> findByFiling_Id(Integer filingId);
    List<Gstr1Cdnra> findByFiling_IdAndGstinOfRecipient(Integer filingId, String gstinOfRecipient);
    void deleteByFiling_Id(Integer filingId);
}