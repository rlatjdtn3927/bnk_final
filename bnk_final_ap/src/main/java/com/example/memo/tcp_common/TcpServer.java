package com.example.memo.tcp_common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/*신경 안써도 되는 클래스*/

@Component
public class TcpServer {
	
	private final TcpMessageRouter router;
	private final ObjectMapper mapper = new ObjectMapper();
	
	public TcpServer(TcpMessageRouter router) {
		this.router = router;
	}
	
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
            String json = in.readLine();
            TcpMessage message = mapper.readValue(json, TcpMessage.class);
            String response = router.route(message);
            
            out.println(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	
	
}
