package com.campusmail.smtp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * SMTP上下文 - 保存单个SMTP会话的状态
 */
@Data
public class SMTPContext {

    public enum State {
        INIT,           // 初始状态
        GREETED,        // 已收到HELO/EHLO
        MAIL_FROM,      // 已收到MAIL FROM
        RCPT_TO,        // 已收到RCPT TO
        DATA            // 正在接收数据
    }

    private State state = State.INIT;
    private String clientDomain;
    private String mailFrom;
    private List<String> rcptTo = new ArrayList<>();
    private StringBuilder dataBuffer = new StringBuilder();

    public void reset() {
        mailFrom = null;
        rcptTo.clear();
        dataBuffer = new StringBuilder();
        if (state != State.INIT) {
            state = State.GREETED;
        }
    }

    public void addRecipient(String recipient) {
        rcptTo.add(recipient);
    }

    public void appendData(String line) {
        dataBuffer.append(line).append("\r\n");
    }

    public String getData() {
        return dataBuffer.toString();
    }
}
