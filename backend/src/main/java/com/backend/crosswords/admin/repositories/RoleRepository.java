package com.backend.crosswords.admin.repositories;

import com.backend.crosswords.admin.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.authorities WHERE r.id = :roleId")
    Optional<Role> findByIdWithAuthorities(@Param("roleId") Long roleId);
}
