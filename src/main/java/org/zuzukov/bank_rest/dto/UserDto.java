package org.zuzukov.bank_rest.dto;


import lombok.Data;

@Data
public class UserDto {
    String userId;
    String firstName;
    String lastName;
    String email;
    String password;
}
