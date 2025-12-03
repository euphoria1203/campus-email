package com.campusmail.smtp;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * SMTP响应对象
 */
@Data
@AllArgsConstructor
public class SMTPResponse {

    private String message;
    private boolean closeConnection;

    public static SMTPResponse ok(String message) {
        return new SMTPResponse(message, false);
    }

    public static SMTPResponse error(String message) {
        return new SMTPResponse(message, false);
    }

    public static SMTPResponse bye(String message) {
        return new SMTPResponse(message, true);
    }
}
