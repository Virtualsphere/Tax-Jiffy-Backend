package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Expa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1ExpaRepository extends JpaRepository<Gstr1Expa, Integer> {
    List<Gstr1Expa> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}