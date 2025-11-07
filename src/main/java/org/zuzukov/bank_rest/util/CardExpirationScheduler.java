package org.zuzukov.bank_rest.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zuzukov.bank_rest.entity.Card;
import org.zuzukov.bank_rest.entity.CardStatus;
import org.zuzukov.bank_rest.repository.CardRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardExpirationScheduler {

    private final CardRepository cardRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void markExpiredCards() {
        List<Card> expired = cardRepository.findAllActiveExpired(CardStatus.ACTIVE, LocalDate.now());
        for (Card card : expired) {
            card.setStatus(CardStatus.EXPIRED);
            log.info("Marked card as expired: id={}, expiry={}", card.getId(), card.getExpiry());
        }
    }
}
