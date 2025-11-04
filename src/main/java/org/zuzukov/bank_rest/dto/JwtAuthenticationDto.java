package org.zuzukov.bank_rest.dto;

import lombok.Data;

@Data
public class JwtAuthenticationDto {
    private String token;
    private String refreshToken;
}

