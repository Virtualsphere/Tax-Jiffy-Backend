package com.gst_reconsilation.user.repository;

import com.gst_reconsilation.user.entity.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Integer> {
    Optional<UserDetails> findByUserEmailAndIsActiveTrue(String email);
    List<UserDetails> findByCompany_IdAndIsActiveTrue(Integer companyId);
    boolean existsByUserEmail(String email);
}