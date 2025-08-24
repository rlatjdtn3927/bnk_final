package com.example.memo.tcp_common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/*신경 안써도 되는 클래스*/

@Component
@RequiredArgsConstructor
public class TcpServer {
	
	private final TcpMessageRouter router;
	private final ObjectMapper mapper;
	private final AES256Util AES256Util;
	
	
	public void start() throws IOException {
		ServerSocket serverSocket = new ServerSocket(50000);
		while(true) {
			Socket client = serverSocket.accept();
			new Thread (() -> handleClient(client)).start();
		}
	}
	
    private void handleClient(Socket socket) {
        try (
        	BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        	PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
        	String encodedRequest = in.readLine();
        	String decodedRequest = AES256Util.decrypt(encodedRequest);
            TcpMessage message = mapper.readValue(decodedRequest, TcpMessage.class);
            
            String response = router.route(message);
            String encodedResponse = AES256Util.encrypt(response);
            out.println(encodedResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	
	
}
