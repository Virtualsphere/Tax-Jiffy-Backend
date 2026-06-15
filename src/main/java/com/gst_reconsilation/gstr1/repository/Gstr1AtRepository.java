package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1At;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1AtRepository extends JpaRepository<Gstr1At, Integer> {
    List<Gstr1At> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}