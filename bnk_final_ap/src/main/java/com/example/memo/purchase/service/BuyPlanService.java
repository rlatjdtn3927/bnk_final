package com.example.memo.purchase.service;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.purchase.trade.BuyPlanFund;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanPG;
import com.example.memo.jpa.entity.user.User;
import com.example.memo.jpa.repository.purchase.trade.BuyPlanFundRepository;
import com.example.memo.jpa.repository.purchase.trade.BuyPlanPGRepository;
import com.example.memo.purchase.dto.trade.CurrentRetainDto;
import com.example.memo.purchase.dto.trade.RetainRequestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuyPlanService {
	
	private final BuyPlanFundRepository buyPlanFundRepo;
	private final BuyPlanPGRepository buyPlanPGRepo;
	private final ObjectMapper mapper;
	
	@Transactional
	public List<CurrentRetainDto> getCurrentRetain(JsonNode data) {
		try {
			RetainRequestDto dto = mapper.treeToValue(data, RetainRequestDto.class);
			User ref = new User();
			ref.setUser_id(dto.getUserId());
			List<BuyPlanFund> fundList = buyPlanFundRepo.findByUserAndIsCurrent(ref, "Y");
			List<BuyPlanPG> PGList = buyPlanPGRepo.findByUserAndIsCurrent(ref, "Y");
			
			List<CurrentRetainDto> result = Stream.of(
					fundList.stream().map(f -> CurrentRetainDto.builder()
							.productId(f.getFund().getProductId())
							.productName(f.getFund().getProductName())
							.fundType(f.getFund().getFundType())
							.riskGradeText(f.getFund().getRiskGradeText())
							.retainRatio(f.getAllocationPercent())
							.build()),
					PGList.stream().map(p -> CurrentRetainDto.builder()
							.productId(p.getPg().getProductId())
							.productName(p.getPg().getBankName() + " " + p.getPg().getProductName() + " " + p.getPg().getMaturityYears())
							.retainRatio(p.getAllocationPercent())
							.build())
					).flatMap(s->s).toList();
			
			return result;	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
