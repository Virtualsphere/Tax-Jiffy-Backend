package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1EcoAB2c;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1EcoAB2cRepository extends JpaRepository<Gstr1EcoAB2c, Integer> {
    List<Gstr1EcoAB2c> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}