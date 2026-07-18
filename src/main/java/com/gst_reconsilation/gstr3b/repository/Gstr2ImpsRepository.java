package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr2Imps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr2ImpsRepository extends JpaRepository<Gstr2Imps, Integer> {
    List<Gstr2Imps> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}