package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1B2b;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1B2bRepository extends JpaRepository<Gstr1B2b, Integer> {
    List<Gstr1B2b> findByFiling_Id(Integer filingId);
    List<Gstr1B2b> findByFiling_IdAndGstinOfRecipient(Integer filingId, String gstinOfRecipient);
    void deleteByFiling_Id(Integer filingId);
}