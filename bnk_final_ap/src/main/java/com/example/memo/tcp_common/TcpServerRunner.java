package com.example.memo.tcp_common;

import java.io.IOException;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/*신경 안써도 되는 클래스*/

@Component
public class TcpServerRunner {

    private final TcpServer tcpServer;

    public TcpServerRunner(TcpServer tcpServer) {
        this.tcpServer = tcpServer;
    }

    @PostConstruct
    public void init() throws IOException {
        new Thread(() -> {
            try {
                tcpServer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}