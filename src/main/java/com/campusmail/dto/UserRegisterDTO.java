package com.campusmail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRegisterDTO {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @Email
    @Pattern(regexp = "^[^@]+@campus\\.mail$", message = "email must end with @campus.mail")
    private String email;
    private String phone;
}
