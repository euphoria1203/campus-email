package com.campusmail.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MailAccount {
    private Long id;
    private Long userId;
    private String emailAddress;
    private String displayName;
    private String smtpHost;
    private Integer smtpPort;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
