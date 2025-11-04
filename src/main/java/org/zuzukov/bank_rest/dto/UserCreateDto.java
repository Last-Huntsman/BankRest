package org.zuzukov.bank_rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCreateDto {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}
