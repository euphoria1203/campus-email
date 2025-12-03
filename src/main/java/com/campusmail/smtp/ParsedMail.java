package com.campusmail.smtp;

import lombok.Data;

import java.util.List;

/**
 * 解析后的邮件对象
 */
@Data
public class ParsedMail {

    private String from;
    private List<String> to;
    private String subject;
    private String body;
    private String rawData;

    // 可扩展：附件列表、Content-Type等
}
