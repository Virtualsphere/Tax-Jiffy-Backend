package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr2Cdnur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr2CdnurRepository extends JpaRepository<Gstr2Cdnur, Integer> {
    List<Gstr2Cdnur> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}