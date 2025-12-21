package com.campusmail.dto;

import lombok.Data;

import java.util.List;

@Data
public class MailDTO {
    private Long id;  // 草稿ID，用于更新现有草稿
    private Long userId;
    private Long accountId;
    private String toAddress;
    private String ccAddress;
    private String bccAddress;
    private String subject;
    private String content;
    private String plainContent;
    private Integer priority;
    private List<Long> attachmentIds;  // 已上传附件的ID列表
}
