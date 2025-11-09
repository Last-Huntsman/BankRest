package org.zuzukov.bank_rest.service;

import org.junit.jupiter.api.Test;
import org.zuzukov.bank_rest.entity.Card;
import org.zuzukov.bank_rest.entity.CardStatus;
import org.zuzukov.bank_rest.exception.custom.BadRequestException;
import org.zuzukov.bank_rest.exception.custom.ConflictException;
import org.zuzukov.bank_rest.util.validator.CardTransferValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CardTransferValidatorTest {

	private final CardTransferValidator validator = new CardTransferValidator();

	@Test
	void ensureNotSameCard_throws() {
		UUID id = UUID.randomUUID();
		assertThrows(BadRequestException.class, () -> validator.ensureNotSameCard(id, id));
	}

	@Test
	void ensureTransferAllowed_notActive() {
		Card c = new Card();
		c.setStatus(CardStatus.BLOCKED);
		c.setExpiry(LocalDate.now().plusDays(1));
		assertThrows(ConflictException.class, () -> validator.ensureTransferFromAllowed(c));
	}

	@Test
	void ensureTransferAllowed_expired() {
		Card c = new Card();
		c.setStatus(CardStatus.ACTIVE);
		c.setExpiry(LocalDate.now().minusDays(1));
		assertThrows(ConflictException.class, () -> validator.ensureTransferToAllowed(c));
	}

	@Test
	void ensureSufficientFunds_ok() {
		validator.ensureSufficientFunds(new BigDecimal("10"), new BigDecimal("5"));
	}

	@Test
	void ensureSufficientFunds_insufficient() {
		assertThrows(BadRequestException.class, () -> validator.ensureSufficientFunds(new BigDecimal("1"), new BigDecimal("5")));
	}
}
