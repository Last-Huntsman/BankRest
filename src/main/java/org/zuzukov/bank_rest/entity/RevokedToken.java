package org.zuzukov.bank_rest.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "revoked_tokens")
@Data
public class RevokedToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "revoked_at", nullable = false)
    private LocalDateTime revokedAt;
}
