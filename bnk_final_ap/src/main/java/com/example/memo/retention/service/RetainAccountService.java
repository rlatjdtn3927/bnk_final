package com.example.memo.retention.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.retention.dto.AccountMinItemDto;
import com.example.memo.retention.dto.RequestAccountListMinDto;
import com.example.memo.retention.dto.ResponseAccountListMinDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RetainAccountService {
    private final IrpAccountRepository irpAccountRepository;
    private final DcAccountRepository  dcAccountRepository;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public ResponseAccountListMinDto listAccountsMin(JsonNode data) {
        try {
            RequestAccountListMinDto dto = mapper.treeToValue(data, RequestAccountListMinDto.class);
            Long userId = dto.getUserId();
            if (userId == null) return null;

            List<AccountMinItemDto> items = new ArrayList<>();

            // IRP (1계좌 가정)
            irpAccountRepository.findByUser_UserId(userId).ifPresent(irp ->
                items.add(AccountMinItemDto.builder()
                        .type("IRP")
                        .id(irp.getIrpAcctNo())
                        .build())
            );

            // DC (계좌번호만 가볍게)
            for (String dcNo : dcAccountRepository.findAccountNosByUserId(userId)) {
                items.add(AccountMinItemDto.builder()
                        .type("DC")
                        .id(dcNo)
                        .build());
            }

            // 기본 계좌: IRP 우선, 없으면 첫번째
            AccountMinItemDto def = items.stream()
                    .filter(a -> "IRP".equals(a.getType()))
                    .findFirst()
                    .orElse(items.isEmpty() ? null : items.get(0));

            return ResponseAccountListMinDto.builder()
                    .accounts(items)
                    .defaults(def)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
