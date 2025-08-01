package com.example.memo.tcp;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

//직렬화/역직렬화 – JSON 한 줄로 끝내면 충분
@Component
@RequiredArgsConstructor
public class MessageCodec {

 private final ObjectMapper om;

 public byte[] encode(Message m) {
     try {
         return om.writeValueAsBytes(m);
     } catch (JsonProcessingException e) {
         throw new IllegalStateException(e);
     }
 }

 public Message decode(byte[] buf) {
     try {
         return om.readValue(buf, Message.class);
     } catch (IOException e) {
         throw new IllegalArgumentException("잘못된 메시지", e);
     }
 }
}