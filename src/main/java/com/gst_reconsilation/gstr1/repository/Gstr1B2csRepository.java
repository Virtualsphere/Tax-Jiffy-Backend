package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1B2cs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1B2csRepository extends JpaRepository<Gstr1B2cs, Integer> {
    List<Gstr1B2cs> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}