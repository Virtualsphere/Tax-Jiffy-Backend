package com.gst_reconsilation.whitebooks.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GstApiSessionRepository extends JpaRepository<GstApiSession, Integer> {
    Optional<GstApiSession> findByCompanyGST_Id(Integer companyGstId);
}
