package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Exp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1ExpRepository extends JpaRepository<Gstr1Exp, Integer> {
    List<Gstr1Exp> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}