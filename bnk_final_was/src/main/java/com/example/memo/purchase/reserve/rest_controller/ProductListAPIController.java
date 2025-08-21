package com.example.memo.purchase.reserve.rest_controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.purchase.reserve.dto.ProductRequestDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/product-api")
@RequiredArgsConstructor
public class ProductListAPIController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper mapper;
	
	@PostMapping("/search")
    public ResponseEntity<?> deposit(@RequestBody ProductRequestDto req) {
		System.out.println("/search...................................");
		System.out.println(req.toString());
    	JsonNode payload = mapper.valueToTree(req);
    	TcpMessage msg = new TcpMessage(Command.PRODUCT_SEARCH, payload);
    	JsonNode response = tcpClientService.sendMessage(msg);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
