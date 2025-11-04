package org.zuzukov.bank_rest.dto.card;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardCreateDto {
    @NotBlank
    private String ownerEmail;

    @Pattern(regexp = "\\d{16}", message = "Card number must contain exactly 16 digits")
    private String cardNumber;

    @Min(1) @Max(12)
    private int expiryMonth;

    @Min(2024)
    private int expiryYear;

    @PositiveOrZero
    private BigDecimal initialBalance;
}


