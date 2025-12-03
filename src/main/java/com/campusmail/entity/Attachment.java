package com.campusmail.entity;

import lombok.Data;

@Data
public class Attachment {
    private Long id;
    private Long mailId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String storagePath;
}
