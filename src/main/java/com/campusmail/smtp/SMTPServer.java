package com.campusmail.smtp;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SMTP服务器 - 监听TCP端口25，使用多线程处理客户端连接
 */
@Component
public class SMTPServer {

    private static final Logger log = LoggerFactory.getLogger(SMTPServer.class);

    @Value("${smtp.port:25}")
    private int port;

    @Value("${smtp.thread-pool-size:10}")
    private int threadPoolSize;

    private final SMTPCommandHandler commandHandler;

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private volatile boolean running = false;
    private Thread acceptThread;

    public SMTPServer(SMTPCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostConstruct
    public void start() {
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        running = true;

        acceptThread = new Thread(this::acceptConnections, "SMTP-Accept-Thread");
        acceptThread.start();
        log.info("SMTP Server started on port {}", port);
    }

    private void acceptConnections() {
        try {
            serverSocket = new ServerSocket(port);
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.info("New SMTP connection from {}", clientSocket.getRemoteSocketAddress());
                    executorService.submit(new SMTPSession(clientSocket, commandHandler));
                } catch (IOException e) {
                    if (running) {
                        log.error("Error accepting connection", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to start SMTP server on port {}", port, e);
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing server socket", e);
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("SMTP Server stopped");
    }
}
