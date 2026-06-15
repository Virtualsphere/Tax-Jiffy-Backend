package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Atadja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1AtadjaRepository extends JpaRepository<Gstr1Atadja, Integer> {
    List<Gstr1Atadja> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}