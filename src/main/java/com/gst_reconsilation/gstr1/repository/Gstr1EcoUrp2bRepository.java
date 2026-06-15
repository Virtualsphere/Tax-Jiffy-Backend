package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1EcoUrp2b;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1EcoUrp2bRepository extends JpaRepository<Gstr1EcoUrp2b, Integer> {
    List<Gstr1EcoUrp2b> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}