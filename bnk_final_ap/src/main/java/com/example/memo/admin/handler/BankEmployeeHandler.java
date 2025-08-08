package com.example.memo.admin.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.memo.admin.service.BankEmployeeService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class BankEmployeeHandler implements TcpMessageHandler {

 @Autowired
 private BankEmployeeService bankEmployeeService;

 @Override
 public boolean supports(Command command) {
     return command.name().startsWith("BANK_EMPLOYEE_");
 }

 @Override
 public Object handle(Command command, JsonNode data) {
     try {
         if (command == Command.BANK_EMPLOYEE_LOGIN) {
             String username = data.get("username").asText();
             String password = data.get("password").asText();
             return bankEmployeeService.verifyLogin(username, password);
         }
         return null;
     } catch (Exception e) {
         e.printStackTrace();
         return null;
     }
 }
}