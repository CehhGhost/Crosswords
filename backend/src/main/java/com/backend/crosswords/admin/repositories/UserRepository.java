package com.backend.crosswords.admin.repositories;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.corpus.models.DocMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsUserByUsername(String username);
    Boolean existsUserByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
}
