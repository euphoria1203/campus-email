package com.campusmail.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String passwordHash;  // 对应数据库 password 字段
    private String email;
    private String nickname;
    private String avatar;
    private Integer status;  // 0-禁用, 1-正常
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
