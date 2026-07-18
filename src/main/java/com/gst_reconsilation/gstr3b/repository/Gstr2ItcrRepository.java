package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr2Itcr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr2ItcrRepository extends JpaRepository<Gstr2Itcr, Integer> {
    List<Gstr2Itcr> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}