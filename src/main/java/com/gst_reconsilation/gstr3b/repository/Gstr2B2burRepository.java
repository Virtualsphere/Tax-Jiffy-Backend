package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr2B2bur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr2B2burRepository extends JpaRepository<Gstr2B2bur, Integer> {
    List<Gstr2B2bur> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}