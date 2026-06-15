package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Atadj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1AtadjRepository extends JpaRepository<Gstr1Atadj, Integer> {
    List<Gstr1Atadj> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}