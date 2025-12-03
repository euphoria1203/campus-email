package com.campusmail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegisterDTO {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @Email
    private String email;
    private String phone;
}
