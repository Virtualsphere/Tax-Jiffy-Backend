package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Eco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1EcoRepository extends JpaRepository<Gstr1Eco, Integer> {
    List<Gstr1Eco> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}