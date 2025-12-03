package com.campusmail.smtp;

import com.campusmail.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMTP命令处理器 - 解析并处理SMTP命令
 * 命令解析与业务逻辑分离
 */
@Component
public class SMTPCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(SMTPCommandHandler.class);

    // 邮箱地址提取正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("<([^>]+)>|([^\\s<>]+@[^\\s<>]+)");

    private final MailService mailService;
    private final MailParser mailParser;

    public SMTPCommandHandler(MailService mailService, MailParser mailParser) {
        this.mailService = mailService;
        this.mailParser = mailParser;
    }

    /**
     * 处理SMTP命令
     */
    public SMTPResponse handleCommand(String line, SMTPContext context, BufferedReader reader) {
        if (line == null || line.isEmpty()) {
            return SMTPResponse.error("500 Syntax error, command unrecognized");
        }

        String command = line.split("\\s+")[0].toUpperCase();
        String argument = line.length() > command.length() ? line.substring(command.length()).trim() : "";

        return switch (command) {
            case "HELO" -> handleHelo(argument, context);
            case "EHLO" -> handleEhlo(argument, context);
            case "MAIL" -> handleMailFrom(argument, context);
            case "RCPT" -> handleRcptTo(argument, context);
            case "DATA" -> handleData(context, reader);
            case "RSET" -> handleRset(context);
            case "NOOP" -> SMTPResponse.ok("250 OK");
            case "QUIT" -> handleQuit(context);
            default -> SMTPResponse.error("500 Syntax error, command unrecognized");
        };
    }

    private SMTPResponse handleHelo(String domain, SMTPContext context) {
        if (domain.isEmpty()) {
            return SMTPResponse.error("501 Syntax: HELO hostname");
        }
        context.setClientDomain(domain);
        context.setState(SMTPContext.State.GREETED);
        return SMTPResponse.ok("250 Hello " + domain + ", pleased to meet you");
    }

    private SMTPResponse handleEhlo(String domain, SMTPContext context) {
        if (domain.isEmpty()) {
            return SMTPResponse.error("501 Syntax: EHLO hostname");
        }
        context.setClientDomain(domain);
        context.setState(SMTPContext.State.GREETED);
        return SMTPResponse.ok("250-CampusMail\r\n250-SIZE 10485760\r\n250 OK");
    }

    private SMTPResponse handleMailFrom(String argument, SMTPContext context) {
        if (context.getState() == SMTPContext.State.INIT) {
            return SMTPResponse.error("503 Error: send HELO/EHLO first");
        }

        // 解析 MAIL FROM:<address>
        if (!argument.toUpperCase().startsWith("FROM:")) {
            return SMTPResponse.error("501 Syntax: MAIL FROM:<address>");
        }

        String emailPart = argument.substring(5).trim();
        String email = extractEmail(emailPart);
        if (email == null) {
            return SMTPResponse.error("501 Syntax: MAIL FROM:<address>");
        }

        context.reset();
        context.setMailFrom(email);
        context.setState(SMTPContext.State.MAIL_FROM);
        return SMTPResponse.ok("250 OK");
    }

    private SMTPResponse handleRcptTo(String argument, SMTPContext context) {
        if (context.getState() != SMTPContext.State.MAIL_FROM && context.getState() != SMTPContext.State.RCPT_TO) {
            return SMTPResponse.error("503 Error: need MAIL command first");
        }

        // 解析 RCPT TO:<address>
        if (!argument.toUpperCase().startsWith("TO:")) {
            return SMTPResponse.error("501 Syntax: RCPT TO:<address>");
        }

        String emailPart = argument.substring(3).trim();
        String email = extractEmail(emailPart);
        if (email == null) {
            return SMTPResponse.error("501 Syntax: RCPT TO:<address>");
        }

        context.addRecipient(email);
        context.setState(SMTPContext.State.RCPT_TO);
        return SMTPResponse.ok("250 OK");
    }

    private SMTPResponse handleData(SMTPContext context, BufferedReader reader) {
        if (context.getState() != SMTPContext.State.RCPT_TO) {
            return SMTPResponse.error("503 Error: need RCPT command first");
        }

        context.setState(SMTPContext.State.DATA);

        // 发送354响应，准备接收数据
        // 注意：这里我们需要先返回354，然后读取数据
        // 由于当前架构，我们在这里直接读取数据

        try {
            StringBuilder data = new StringBuilder();
            String line;

            // 客户端收到354后开始发送数据，以单独的"."结束
            while ((line = reader.readLine()) != null) {
                if (".".equals(line)) {
                    break;
                }
                // 处理透明传输（行首的点需要去掉一个）
                if (line.startsWith("..")) {
                    line = line.substring(1);
                }
                data.append(line).append("\r\n");
            }

            // 解析并保存邮件
            ParsedMail parsedMail = mailParser.parse(data.toString(), context.getMailFrom(), context.getRcptTo());
            mailService.createMail(parsedMail);

            context.reset();
            return SMTPResponse.ok("250 OK: Message queued");

        } catch (IOException e) {
            log.error("Error reading DATA", e);
            return SMTPResponse.error("451 Requested action aborted: error in processing");
        } catch (Exception e) {
            log.error("Error processing mail", e);
            return SMTPResponse.error("451 Requested action aborted: error in processing");
        }
    }

    private SMTPResponse handleRset(SMTPContext context) {
        context.reset();
        return SMTPResponse.ok("250 OK");
    }

    private SMTPResponse handleQuit(SMTPContext context) {
        return SMTPResponse.bye("221 Bye");
    }

    /**
     * 从字符串中提取邮箱地址
     */
    private String extractEmail(String input) {
        Matcher matcher = EMAIL_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        return null;
    }
}
