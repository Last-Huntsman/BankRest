package org.zuzukov.bank_rest.util.validator;

import org.springframework.stereotype.Component;
import org.zuzukov.bank_rest.entity.Card;
import org.zuzukov.bank_rest.entity.CardStatus;
import org.zuzukov.bank_rest.exception.custom.BadRequestException;
import org.zuzukov.bank_rest.exception.custom.ConflictException;

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

    public void ensureTransferFromAllowed(Card card) {
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new ConflictException("Cannot transfer from blocked card");
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new ConflictException("Card is expired");
        }
    }

    public void ensureTransferToAllowed(Card card) {
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new ConflictException("Destination card is not active");
        }
        if (card.getExpiry().isBefore(LocalDate.now())) {
            throw new ConflictException("Destination card is expired");
        }
    }

    public void ensureSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient funds");
        }
    }
}
