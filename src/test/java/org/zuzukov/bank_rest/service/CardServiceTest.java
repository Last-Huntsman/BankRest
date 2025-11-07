package org.zuzukov.bank_rest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.zuzukov.bank_rest.dto.card.CardCreateDto;
import org.zuzukov.bank_rest.dto.card.CardDto;
import org.zuzukov.bank_rest.dto.card.TransferRequestDto;
import org.zuzukov.bank_rest.entity.Card;
import org.zuzukov.bank_rest.entity.CardStatus;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.exception.NotFoundException;
import org.zuzukov.bank_rest.mapper.CardMapper;
import org.zuzukov.bank_rest.repository.CardRepository;
import org.zuzukov.bank_rest.repository.UserRepository;
import org.zuzukov.bank_rest.service.crypto.CryptoService;
import org.zuzukov.bank_rest.service.validator.CardTransferValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CardServiceTest {

	private CardRepository cardRepository;
	private UserRepository userRepository;
	private CryptoService cryptoService;
	private CardMapper cardMapper;
	private CardTransferValidator validator;
	private CardService cardService;

	@BeforeEach
	void setup() {
		cardRepository = Mockito.mock(CardRepository.class);
		userRepository = Mockito.mock(UserRepository.class);
		cryptoService = Mockito.mock(CryptoService.class);
		cardMapper = Mockito.mock(CardMapper.class);
		validator = Mockito.mock(CardTransferValidator.class);
		cardService = new CardService(cardRepository, userRepository, cryptoService, cardMapper, validator);
	}

	@Test
	void adminCreate_success() {
		CardCreateDto dto = new CardCreateDto();
		dto.setOwnerEmail("a@b.c");
		dto.setCardNumber("4111111111111111");
		dto.setExpiryMonth(12);
		dto.setExpiryYear(2030);
		dto.setInitialBalance(new BigDecimal("100.00"));

		User owner = new User(); owner.setEmail("a@b.c");
		when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(owner));
		when(cryptoService.encrypt("4111111111111111")).thenReturn("enc");
		when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
			Card c = inv.getArgument(0);
			c.setId(UUID.randomUUID());
			return c;
		});
		when(cardMapper.toDto(any())).thenReturn(new CardDto());

		CardDto result = cardService.adminCreate(dto);
		assertNotNull(result);
		ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
		verify(cardRepository).save(captor.capture());
		Card saved = captor.getValue();
		assertEquals("enc", saved.getNumberEncrypted());
		assertEquals("1111", saved.getLast4());
		assertEquals(CardStatus.ACTIVE, saved.getStatus());
	}

	@Test
	void adminBlock_notFound() {
		UUID id = UUID.randomUUID();
		when(cardRepository.findById(id)).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> cardService.adminBlock(id));
	}

	@Test
	void adminActivate_success() {
		UUID id = UUID.randomUUID();
		Card c = new Card(); c.setId(id);
		when(cardRepository.findById(id)).thenReturn(Optional.of(c));
		cardService.adminActivate(id);
		assertEquals(CardStatus.ACTIVE, c.getStatus());
	}

	@Test
	void userListOwn_setsExpiredStatusInDto() {
		Card c = new Card();
		c.setExpiry(LocalDate.now().minusDays(1));
		c.setStatus(CardStatus.ACTIVE);
		CardDto cd = new CardDto();
		cd.setExpiry(LocalDate.now().minusDays(1));
		cd.setStatus(CardStatus.ACTIVE);
		when(cardRepository.findAllByOwnerEmail(eq("u@x"), any())).thenReturn(new PageImpl<>(List.of(c)));
		when(cardMapper.toDto(any())).thenReturn(cd);

		Page<CardDto> page = cardService.userListOwn("u@x", null, PageRequest.of(0, 10));
		assertThat(page.getContent().get(0).getStatus()).isEqualTo(CardStatus.EXPIRED);
	}

	@Test
	void userRequestBlock_ignoresExpired() {
		UUID id = UUID.randomUUID();
		Card c = new Card(); c.setStatus(CardStatus.EXPIRED);
		when(cardRepository.findByIdAndOwnerEmail(id, "u@x")).thenReturn(Optional.of(c));
		cardService.userRequestBlock("u@x", id);
		assertEquals(CardStatus.EXPIRED, c.getStatus());
	}

	@Test
	void transferBetweenOwn_success() {
		UUID fromId = UUID.randomUUID();
		UUID toId = UUID.randomUUID();
		Card from = new Card(); from.setId(fromId); from.setStatus(CardStatus.ACTIVE); from.setExpiry(LocalDate.now().plusDays(1)); from.setBalance(new BigDecimal("100"));
		Card to = new Card(); to.setId(toId); to.setStatus(CardStatus.ACTIVE); to.setExpiry(LocalDate.now().plusDays(1)); to.setBalance(new BigDecimal("0"));
		when(cardRepository.findByIdAndOwnerEmail(fromId, "u@x")).thenReturn(Optional.of(from));
		when(cardRepository.findByIdAndOwnerEmail(toId, "u@x")).thenReturn(Optional.of(to));

		TransferRequestDto req = new TransferRequestDto();
		req.setFromCardId(fromId);
		req.setToCardId(toId);
		req.setAmount(new BigDecimal("25"));

		cardService.transferBetweenOwn("u@x", req);

		verify(validator).ensureNotSameCard(fromId, toId);
		verify(validator, times(2)).ensureTransferAllowed(any(Card.class));
		verify(validator).ensureSufficientFunds(new BigDecimal("100"), new BigDecimal("25"));
		assertEquals(new BigDecimal("75"), from.getBalance());
		assertEquals(new BigDecimal("25"), to.getBalance());
	}

	@Test
	void adminSearch_delegatesToRepo() {
		when(cardRepository.adminSearch(any(), any(), any(), any())).thenReturn(Page.empty());
		Page<CardDto> res = cardService.adminSearch(null, null, null, PageRequest.of(0, 10));
		assertNotNull(res);
	}
}
