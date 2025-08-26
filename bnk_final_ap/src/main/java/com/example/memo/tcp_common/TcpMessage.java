package com.example.memo.tcp_common;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*이 형식으로 메세지를 받아야함.*/

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TcpMessage {
    private Command command;
    private JsonNode data;
}



