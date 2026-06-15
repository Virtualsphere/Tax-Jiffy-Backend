package com.gst_reconsilation.gstr1.repository;

import com.gst_reconsilation.gstr1.entity.Gstr1HsnB2c;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Gstr1HsnB2cRepository extends JpaRepository<Gstr1HsnB2c, Integer> {
    List<Gstr1HsnB2c> findByFiling_Id(Integer filingId);
    List<Gstr1HsnB2c> findByFiling_IdAndHsn(Integer filingId, String hsn);
    void deleteByFiling_Id(Integer filingId);
}