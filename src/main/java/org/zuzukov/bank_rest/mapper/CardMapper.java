package org.zuzukov.bank_rest.mapper;

import org.springframework.stereotype.Component;
import org.zuzukov.bank_rest.dto.card.CardDto;
import org.zuzukov.bank_rest.entity.Card;
import org.zuzukov.bank_rest.util.MaskingUtil;

@Component
public class CardMapper {
    public CardDto toDto(Card c) {
        CardDto dto = new CardDto();
        dto.setId(c.getId());
        dto.setOwnerEmail(c.getOwner().getEmail());
        dto.setExpiry(c.getExpiry());
        dto.setStatus(c.getStatus());
        dto.setBalance(c.getBalance());
        dto.setMaskedNumber(MaskingUtil.maskCardLast4(c.getLast4()));
        return dto;
    }
}


