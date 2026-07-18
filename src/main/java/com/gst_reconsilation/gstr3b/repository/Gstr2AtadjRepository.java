package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr2Atadj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr2AtadjRepository extends JpaRepository<Gstr2Atadj, Integer> {
    List<Gstr2Atadj> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}