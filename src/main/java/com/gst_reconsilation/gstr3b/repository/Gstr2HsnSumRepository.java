package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr2HsnSum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr2HsnSumRepository extends JpaRepository<Gstr2HsnSum, Integer> {
    List<Gstr2HsnSum> findByFiling_Id(Integer filingId);
    List<Gstr2HsnSum> findByFiling_IdAndHsn(Integer filingId, String hsn);
    void deleteByFiling_Id(Integer filingId);
}