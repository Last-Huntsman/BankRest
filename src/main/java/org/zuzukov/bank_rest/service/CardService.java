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
    private final UserRepository userRepository;
    private final CryptoService cryptoService;
    private final CardMapper cardMapper;
    private final CardTransferValidator transferValidator;


    @Transactional
    public CardDto adminCreate(CardCreateDto dto) {
        User owner = userRepository.findByEmail(dto.getOwnerEmail())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));

        Card card = new Card();
        card.setOwner(owner);
        card.setNumberEncrypted(cryptoService.encrypt(dto.getCardNumber()));
        card.setLast4(dto.getCardNumber().substring(12));
        YearMonth ym = YearMonth.of(dto.getExpiryYear(), dto.getExpiryMonth());
        card.setExpiry(ym.atEndOfMonth());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(dto.getInitialBalance() == null ? BigDecimal.ZERO : dto.getInitialBalance());
        card = cardRepository.save(card);
        log.info("Card created for owner={}, cardId={}, last4={}", owner.getEmail(), card.getId(), card.getLast4());
        return cardMapper.toDto(card);
    }


    @Transactional
    public void adminBlock(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        card.setStatus(CardStatus.BLOCKED);
        log.info("Card blocked: id={}", cardId);
    }


    @Transactional
    public void adminActivate(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        card.setStatus(CardStatus.ACTIVE);
        log.info("Card activated: id={}", cardId);
    }

    @Transactional
    public void adminDelete(UUID cardId) {
        cardRepository.deleteById(cardId);
        log.info("Card deleted: id={}", cardId);
    }


    @Transactional(readOnly = true)
    public Page<CardDto> userListOwn(String userEmail, CardStatus status, Pageable pageable) {
        LocalDate today = LocalDate.now();
        Page<CardDto> page = (status == null
                ? cardRepository.findAllByOwnerEmail(userEmail, pageable)
                : cardRepository.findAllByOwnerEmailAndStatus(userEmail, status, pageable))
                .map(cardMapper::toDto);

        page.forEach(c -> {
            if (c.getExpiry().isBefore(today) && c.getStatus() == CardStatus.ACTIVE) {
                c.setStatus(CardStatus.EXPIRED);
            }
        });
        log.debug("Cards listed for user={}, count={}", userEmail, page.getNumberOfElements());
        return page;
    }


    @Transactional
    public void userRequestBlock(String userEmail, UUID cardId) {
        Card card = cardRepository.findByIdAndOwnerEmail(cardId, userEmail).orElseThrow();
        if (card.getStatus() == CardStatus.EXPIRED) return;
        card.setStatus(CardStatus.BLOCKED);
        log.info("User requested block: user={}, cardId={}", userEmail, cardId);
    }


    @Transactional
    public void transferBetweenOwn(String userEmail, TransferRequestDto transfer) {
        transferValidator.ensureNotSameCard(transfer.getFromCardId(), transfer.getToCardId());
        Card from = cardRepository.findByIdAndOwnerEmail(transfer.getFromCardId(), userEmail)
                .orElseThrow(() -> new NotFoundException("From card not found"));
        Card to = cardRepository.findByIdAndOwnerEmail(transfer.getToCardId(), userEmail)
                .orElseThrow(() -> new NotFoundException("To card not found"));

        transferValidator.ensureTransferAllowed(from);
        transferValidator.ensureTransferAllowed(to);

        BigDecimal amount = transfer.getAmount();
        transferValidator.ensureSufficientFunds(from.getBalance(), amount);

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        log.info("Transfer: user={}, fromCard={}, toCard={}, amount={}", userEmail, from.getId(), to.getId(), amount);
    }


    @Transactional(readOnly = true)
    public Page<CardDto> adminSearch(String ownerEmail, CardStatus status, String last4, Pageable pageable) {
        return cardRepository.findAllByOwnerEmail(ownerEmail, status, last4, pageable).map(cardMapper::toDto);
    }


}


