package com.campusmail.entity;

import lombok.Data;

@Data
public class Contact {
    private Long id;
    private Long userId;
    private String contactName;
    private String email;
    private String phone;
    private String notes;
}
