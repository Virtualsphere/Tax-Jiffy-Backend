package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Cdnura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1CdnuraRepository extends JpaRepository<Gstr1Cdnura, Integer> {
    List<Gstr1Cdnura> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}