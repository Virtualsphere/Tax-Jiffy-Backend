package com.gst_reconsilation.roles.repository;

import com.gst_reconsilation.roles.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RolesRepository extends JpaRepository<Roles, Integer> {
    List<Roles> findByIsActiveTrue();
}