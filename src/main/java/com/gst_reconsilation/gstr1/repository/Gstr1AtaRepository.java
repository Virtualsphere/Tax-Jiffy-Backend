package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Ata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1AtaRepository extends JpaRepository<Gstr1Ata, Integer> {
    List<Gstr1Ata> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}