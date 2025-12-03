package com.campusmail.dto;

import lombok.Data;

@Data
public class AttachmentDTO {
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String base64Content;
}
