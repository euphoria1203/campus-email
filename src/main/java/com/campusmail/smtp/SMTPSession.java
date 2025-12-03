package com.campusmail.smtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * SMTP会话处理 - 处理单个客户端连接的完整SMTP交互
 */
public class SMTPSession implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SMTPSession.class);

    private final Socket socket;
    private final SMTPCommandHandler commandHandler;

    public SMTPSession(Socket socket, SMTPCommandHandler commandHandler) {
        this.socket = socket;
        this.commandHandler = commandHandler;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            // 发送欢迎消息
            writer.println("220 CampusMail SMTP Server Ready");

            SMTPContext context = new SMTPContext();
            String line;

            while ((line = reader.readLine()) != null) {
                log.debug("SMTP received: {}", line);

                SMTPResponse response = commandHandler.handleCommand(line, context, reader);
                writer.println(response.getMessage());

                if (response.isCloseConnection()) {
                    break;
                }
            }
        } catch (IOException e) {
            log.error("SMTP session error", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.error("Error closing socket", e);
            }
        }
    }
}
