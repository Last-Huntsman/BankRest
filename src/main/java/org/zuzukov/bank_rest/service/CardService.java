package org.zuzukov.bank_rest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zuzukov.bank_rest.dto.card.CardCreateDto;
import org.zuzukov.bank_rest.dto.card.CardDto;
import org.zuzukov.bank_rest.dto.card.TransferRequestDto;
import org.zuzukov.bank_rest.entity.Card;
import org.zuzukov.bank_rest.entity.CardStatus;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.repository.CardRepository;
import org.zuzukov.bank_rest.repository.UserRepository;
import org.zuzukov.bank_rest.service.crypto.CryptoService;
import org.zuzukov.bank_rest.exception.NotFoundException;
import org.zuzukov.bank_rest.mapper.CardMapper;
import org.zuzukov.bank_rest.service.validator.CardTransferValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    @Transactional(readOnly = true)
    public Page<CardDto> adminSearch(String ownerEmail, CardStatus status, String last4, Pageable pageable) {
        return cardRepository.findAll(pageable).map(cardMapper::toDto);
    }

    @Transactional
    public void adminBlock(UUID cardId) {
        Card card = cardRepository.findById(cardId).orElseThrow();
        card.setStatus(CardStatus.BLOCKED);
    }
}



