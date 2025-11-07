package org.zuzukov.t1task4.service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.zuzukov.bank_rest.exception.BadRequestException;
import org.zuzukov.bank_rest.dto.card.TransferRequestDto;
import org.zuzukov.bank_rest.entity.Card;
import org.zuzukov.bank_rest.entity.CardStatus;
import org.zuzukov.bank_rest.entity.Role;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.mapper.CardMapper;
import org.zuzukov.bank_rest.repository.CardRepository;
import org.zuzukov.bank_rest.repository.UserRepository;
import org.zuzukov.bank_rest.service.CardService;
import org.zuzukov.bank_rest.service.crypto.CryptoService;
import org.zuzukov.bank_rest.service.validator.CardTransferValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class CardServiceTest {

    private CardRepository cardRepository;
    private UserRepository userRepository;
    private CryptoService cryptoService;
    private CardService service;
    private CardMapper cardMapper;
    private CardTransferValidator transferValidator;

    private User user;
    private Card cardFrom;
    private Card cardTo;

    @BeforeEach
    void setUp() {
        cardRepository = Mockito.mock(CardRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        cryptoService = Mockito.mock(CryptoService.class);
        cardMapper = new CardMapper();
        transferValidator = new CardTransferValidator();
        service = new CardService(cardRepository, userRepository, cryptoService, cardMapper, transferValidator);

        user = new User();
        user.setEmail("user@example.com");
        user.setRoles(Set.of(Role.ROLE_GUEST));

        cardFrom = new Card();
        cardFrom.setId(UUID.randomUUID());
        cardFrom.setOwner(user);
        cardFrom.setStatus(CardStatus.ACTIVE);
        cardFrom.setExpiry(LocalDate.now().plusMonths(1));
        cardFrom.setBalance(new BigDecimal("100.00"));

        cardTo = new Card();
        cardTo.setId(UUID.randomUUID());
        cardTo.setOwner(user);
        cardTo.setStatus(CardStatus.ACTIVE);
        cardTo.setExpiry(LocalDate.now().plusMonths(1));
        cardTo.setBalance(new BigDecimal("10.00"));
    }

    @Test
    void transfer_ok() {
        when(cardRepository.findByIdAndOwnerEmail(cardFrom.getId(), user.getEmail())).thenReturn(Optional.of(cardFrom));
        when(cardRepository.findByIdAndOwnerEmail(cardTo.getId(), user.getEmail())).thenReturn(Optional.of(cardTo));

        TransferRequestDto req = new TransferRequestDto();
        req.setFromCardId(cardFrom.getId());
        req.setToCardId(cardTo.getId());
        req.setAmount(new BigDecimal("25.50"));

        service.transferBetweenOwn(user.getEmail(), req);

        assertEquals(new BigDecimal("74.50"), cardFrom.getBalance());
        assertEquals(new BigDecimal("35.50"), cardTo.getBalance());
    }

    @Test
    void transfer_insufficientFunds() {
        when(cardRepository.findByIdAndOwnerEmail(cardFrom.getId(), user.getEmail())).thenReturn(Optional.of(cardFrom));
        when(cardRepository.findByIdAndOwnerEmail(cardTo.getId(), user.getEmail())).thenReturn(Optional.of(cardTo));

        TransferRequestDto req = new TransferRequestDto();
        req.setFromCardId(cardFrom.getId());
        req.setToCardId(cardTo.getId());
        req.setAmount(new BigDecimal("1000.00"));

        assertThrows(BadRequestException.class, () -> service.transferBetweenOwn(user.getEmail(), req));
    }

    @Test
    void transfer_sameCard_forbidden() {
        TransferRequestDto req = new TransferRequestDto();
        req.setFromCardId(cardFrom.getId());
        req.setToCardId(cardFrom.getId());
        req.setAmount(new BigDecimal("1.00"));

        assertThrows(BadRequestException.class, () -> service.transferBetweenOwn(user.getEmail(), req));
        verify(cardRepository, never()).findByIdAndOwnerEmail(ArgumentMatchers.any(), ArgumentMatchers.anyString());
    }
}


