package org.zuzukov.bank_rest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zuzukov.bank_rest.entity.Card;
import org.zuzukov.bank_rest.entity.CardStatus;

import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {


    Page<Card> findAllByOwnerEmail(String email, Pageable pageable);

    Page<Card> findAllByOwnerEmailAndStatus(String email, CardStatus status, Pageable pageable);

    Optional<Card> findByIdAndOwnerEmail(UUID cardId, String email);

    @Query("""
                select c from Card c
                where c.owner.email = coalesce(:ownerEmail, c.owner.email)
                  and c.status = coalesce(:status, c.status)
                  and c.last4 = coalesce(:last4, c.last4)
            """)
    Page<Card> adminSearch(@Param("ownerEmail") String ownerEmail,
                           @Param("status") CardStatus status,
                           @Param("last4") String last4,
                           Pageable pageable);
}


