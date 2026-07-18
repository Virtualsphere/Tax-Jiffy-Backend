package com.gst_reconsilation.gstr3b.repository;

import com.gst_reconsilation.gstr3b.entity.Gstr2At;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr2AtRepository extends JpaRepository<Gstr2At, Integer> {
    List<Gstr2At> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}