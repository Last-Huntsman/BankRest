package org.zuzukov.bank_rest.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.zuzukov.bank_rest.entity.RevokedToken;


import java.util.Optional;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    Optional<RevokedToken> findByToken(String token);

    boolean existsByToken(String tokenHash);
}
