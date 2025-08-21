package com.example.memo.purchase.reserve.service;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanFund;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanPG;
import com.example.memo.jpa.entity.user.UserEntity;
import com.example.memo.jpa.repository.purchase.trade.BuyPlanFundRepository;
import com.example.memo.jpa.repository.purchase.trade.BuyPlanPGRepository;
import com.example.memo.purchase.reserve.dto.request.ReserveUpdateRequestDto;
import com.example.memo.purchase.reserve.dto.request.RetainRequestDto;
import com.example.memo.purchase.reserve.dto.request.SourceProdRatioDto;
import com.example.memo.purchase.reserve.dto.response.CurrentRetainDto;
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
			UserEntity ref = new UserEntity();
			ref.setUserId(dto.getUserId());
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
	
	@Transactional
	public String updateBuyPlan(JsonNode data) {
		try {
			ReserveUpdateRequestDto request = mapper.treeToValue(data, ReserveUpdateRequestDto.class);
			Long userId = request.getUserId();
			List<String> targetProdIdList = request.getTargetProdIdList();
			List<SourceProdRatioDto> sourceProdList = request.getSourceProdList();
			
			List<BuyPlanFund> fundEntities = buyPlanFundRepo.findByFund_ProductIdIn(targetProdIdList);
			// 타겟 아이디 엔티티 상태값 N으로 수정
			if(fundEntities != null) {
				for(BuyPlanFund entity : fundEntities) {
					entity.setIsCurrent("N");
					buyPlanFundRepo.save(entity);
				}
			}
			
			List<BuyPlanPG> pgEntities = buyPlanPGRepo.findByPrincipal_ProductIdIn(targetProdIdList);
			if(pgEntities != null) {
				for(BuyPlanPG entity : pgEntities) {
					entity.setIsCurrent("N");
					buyPlanPGRepo.save(entity);
				}
			}
			
			// 새로운 BuyPlan 등록
			for(SourceProdRatioDto dto : sourceProdList) {
				String prodId = dto.getProdId();
				Integer ratio = dto.getRatio();
				
				if(prodId.startsWith("PG")) {
					BuyPlanPG newEntity = BuyPlanPG.builder()
							.user(UserEntity.builder().userId(userId).build())
							.pg(PrincipalGuarantee.builder().productId(prodId).build())
							.allocationPercent(ratio)
							.isCurrent("Y")
							.build(); 
					buyPlanPGRepo.save(newEntity);
				} else { //fund인 경우
					BuyPlanFund newEntity = BuyPlanFund.builder()
							.user(UserEntity.builder().userId(userId).build())
							.fund(FundMaster.builder().productId(prodId).build())
							.allocationPercent(ratio)
							.isCurrent("Y")
							.build();
					buyPlanFundRepo.save(newEntity);
				}
				
			return "매수상품 예정 등록이 완료되었습니다.";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "매수 상품 예정 업데이트 중 오류 발생";
		}
		return "매수 상품 예정 업데이트 중 오류 발생";
	}
}
