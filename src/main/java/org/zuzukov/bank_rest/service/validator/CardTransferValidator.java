package org.zuzukov.bank_rest.service.validator;

import org.springframework.stereotype.Component;
import org.zuzukov.bank_rest.entity.Card;
import org.zuzukov.bank_rest.entity.CardStatus;
import org.zuzukov.bank_rest.exception.BadRequestException;
import org.zuzukov.bank_rest.exception.ConflictException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class CardTransferValidator {
    public void ensureNotSameCard(UUID fromId, UUID toId) {
        if (fromId.equals(toId)) {
            throw new BadRequestException("Cannot transfer to the same card");
        }
    }

    public void ensureTransferAllowed(Card card) {
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new ConflictException("Card is not active");
        }
        if (card.getExpiry().isBefore(LocalDate.now())) {
            throw new ConflictException("Card expired");
        }
    }

    public void ensureSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient funds");
        }
    }
}


