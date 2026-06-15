package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1Docs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1DocsRepository extends JpaRepository<Gstr1Docs, Integer> {
    List<Gstr1Docs> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}