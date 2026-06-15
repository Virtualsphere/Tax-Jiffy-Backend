package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1EcoB2b;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1EcoB2bRepository extends JpaRepository<Gstr1EcoB2b, Integer> {
    List<Gstr1EcoB2b> findByFiling_Id(Integer filingId);
    void deleteByFiling_Id(Integer filingId);
}