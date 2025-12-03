package com.campusmail.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Mail {
    private Long id;
    private Long userId;
    private Long accountId;
    private String folder;          // inbox/sent/drafts/trash
    private String fromAddress;
    private String toAddress;
    private String ccAddress;
    private String bccAddress;
    private String subject;
    private String content;         // HTML内容
    private String plainContent;    // 纯文本内容
    private Boolean isRead;
    private Boolean isStarred;
    private Boolean isDeleted;
    private Boolean hasAttachment;
    private Integer priority;       // 1-高, 3-普通, 5-低
    private LocalDateTime sendTime;
    private LocalDateTime receiveTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
