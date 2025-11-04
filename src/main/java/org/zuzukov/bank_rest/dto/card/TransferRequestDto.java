package org.zuzukov.bank_rest.dto.card;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferRequestDto {
    @NotNull
    private UUID fromCardId;

    @NotNull
    private UUID toCardId;

    @Positive
    private BigDecimal amount;
}


