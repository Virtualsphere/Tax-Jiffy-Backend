package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1B2cla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1B2claRepository extends JpaRepository<Gstr1B2cla, Integer> {
    List<Gstr1B2cla> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}