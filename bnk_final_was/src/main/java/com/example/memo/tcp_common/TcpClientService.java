package com.example.memo.tcp_common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TcpClientService {

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode sendMessage(TcpMessage message) {
        try (
            Socket socket = new Socket("localhost", 9000);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            String json = mapper.writeValueAsString(message);
            out.println(json);

            String response = in.readLine();
            return mapper.readTree(response);

        } catch (Exception e) {
            e.printStackTrace();
            return "에러 발생";
        }
    }
}