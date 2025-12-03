package com.campusmail.smtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 邮件解析器 - 解析SMTP DATA部分的邮件内容
 * 支持基本的邮件头和正文解析
 */
@Component
public class MailParser {

    private static final Logger log = LoggerFactory.getLogger(MailParser.class);

    /**
     * 解析邮件内容
     * @param rawData 原始邮件数据（包含头部和正文）
     * @param envelopeFrom SMTP信封发件人
     * @param envelopeTo SMTP信封收件人列表
     * @return 解析后的邮件对象
     */
    public ParsedMail parse(String rawData, String envelopeFrom, List<String> envelopeTo) {
        ParsedMail mail = new ParsedMail();
        mail.setRawData(rawData);
        mail.setFrom(envelopeFrom);
        mail.setTo(envelopeTo);

        // 分离头部和正文（空行分隔）
        int headerEnd = rawData.indexOf("\r\n\r\n");
        if (headerEnd == -1) {
            headerEnd = rawData.indexOf("\n\n");
        }

        String headers;
        String body;

        if (headerEnd != -1) {
            headers = rawData.substring(0, headerEnd);
            body = rawData.substring(headerEnd).trim();
        } else {
            headers = "";
            body = rawData;
        }

        // 解析头部
        mail.setSubject(extractHeader(headers, "Subject"));

        // 如果头部中有From，可以覆盖信封地址
        String headerFrom = extractHeader(headers, "From");
        if (headerFrom != null && !headerFrom.isEmpty()) {
            mail.setFrom(headerFrom);
        }

        mail.setBody(body);

        log.debug("Parsed mail: from={}, to={}, subject={}", mail.getFrom(), mail.getTo(), mail.getSubject());

        return mail;
    }

    /**
     * 从邮件头中提取指定字段值
     */
    private String extractHeader(String headers, String headerName) {
        String[] lines = headers.split("\r?\n");
        StringBuilder value = new StringBuilder();
        boolean found = false;

        for (String line : lines) {
            if (line.toLowerCase().startsWith(headerName.toLowerCase() + ":")) {
                found = true;
                value.append(line.substring(headerName.length() + 1).trim());
            } else if (found && (line.startsWith(" ") || line.startsWith("\t"))) {
                // 折叠行继续
                value.append(" ").append(line.trim());
            } else if (found) {
                break;
            }
        }

        return value.length() > 0 ? value.toString() : null;
    }
}
