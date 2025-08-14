package com.example.memo.tcp_common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/*메세지 전송 시에 사용해야 할 클래스 함수 구현 자체는 신경쓰지 마세요*/
@Service
public class TcpClientService {
	
    private final ObjectMapper mapper = new ObjectMapper();
    
    /*이 함수 쓰면 됨 --> 요청하면 응답까지 받음*/
    public JsonNode sendMessage(TcpMessage message) {
        try (
            Socket socket = new Socket("localhost", 50000);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            String json = mapper.writeValueAsString(message);
            String encodedString = AES256Util.encrypt(json);
            out.println(encodedString);

            String response = in.readLine();
            return mapper.readTree(response);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}