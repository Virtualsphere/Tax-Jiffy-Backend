package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1B2csa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1B2csaRepository extends JpaRepository<Gstr1B2csa, Integer> {
    List<Gstr1B2csa> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}