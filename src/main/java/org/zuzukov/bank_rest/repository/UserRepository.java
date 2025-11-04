package org.zuzukov.bank_rest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zuzukov.bank_rest.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUserId(UUID id);
    Optional<User> findByEmail(String email);
    Page<User> findAll(Pageable pageable);
}
