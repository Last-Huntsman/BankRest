package org.zuzukov.bank_rest.dto.card;

import lombok.Data;
import org.zuzukov.bank_rest.entity.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CardDto {
    private UUID id;
    private String maskedNumber;
    private String ownerEmail;
    private LocalDate expiry;
    private CardStatus status;
    private BigDecimal balance;
}


