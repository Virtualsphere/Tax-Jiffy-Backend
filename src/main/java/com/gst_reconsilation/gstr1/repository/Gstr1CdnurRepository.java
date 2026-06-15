package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Cdnur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1CdnurRepository extends JpaRepository<Gstr1Cdnur, Integer> {
    List<Gstr1Cdnur> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}