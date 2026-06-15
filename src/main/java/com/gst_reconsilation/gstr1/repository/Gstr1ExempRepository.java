package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Exemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1ExempRepository extends JpaRepository<Gstr1Exemp, Integer> {
    List<Gstr1Exemp> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}