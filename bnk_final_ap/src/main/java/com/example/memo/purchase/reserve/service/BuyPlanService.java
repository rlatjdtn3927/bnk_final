package com.example.memo.purchase.reserve.service;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanFund;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanPG;
import com.example.memo.jpa.entity.user.UserEntity;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.purchase.trade.BuyPlanFundRepository;
import com.example.memo.jpa.repository.purchase.trade.BuyPlanPGRepository;
import com.example.memo.purchase.common.dto.CurrentRetainDto;
import com.example.memo.purchase.reserve.dto.request.ReserveUpdateRequestDto;
import com.example.memo.purchase.reserve.dto.request.RetainRequestDto;
import com.example.memo.purchase.reserve.dto.request.SourceProdRatioDto;
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
	private final DcAccountRepository dcAccountRepository;
	private final ObjectMapper mapper;
	
	@Transactional
	public List<CurrentRetainDto> getCurrentRetain(JsonNode data) {
		try {
			RetainRequestDto dto = mapper.treeToValue(data, RetainRequestDto.class);
			Long userId = dto.getUserId();
			String accountType = dto.getAccountType();
			String accountNo = dto.getAccountId();
			UserEntity ref = new UserEntity();
			ref.setUserId(userId);
			List<BuyPlanFund> fundList = null;
			List<BuyPlanPG> PGList = null;
			
			if("DC".equals(accountType)) {
				fundList = buyPlanFundRepo.findByUserAndIsCurrentAndDcAccount_AccountNo(ref, "Y", accountNo);
				PGList = buyPlanPGRepo.findByUserAndIsCurrentAndDcAccount_AccountNo(ref, "Y", accountNo);
			} else {
				fundList = buyPlanFundRepo.findByUserAndIsCurrentAndIrpAccount_IrpAcctNo(ref, "Y", accountNo);
				PGList= buyPlanPGRepo.findByUserAndIsCurrentAndIrpAccount_IrpAcctNo(ref, "Y", accountNo);
			}
			
			List<CurrentRetainDto> result = Stream.of(
					fundList.stream().map(f -> CurrentRetainDto.builder()
							.productId(f.getFund().getProductId())
							.productName(f.getFund().getProductName())
							.fundType(f.getFund().getFundType())
							.riskGradeText(f.getFund().getRiskGradeText())
							.retainRatio(f.getAllocationPercent())
							.build()),
					PGList.stream().map(p -> CurrentRetainDto.builder()
							.productId(p.getPrincipal().getProductId())
							.productName(p.getPrincipal().getBankName() + " " + p.getPrincipal().getProductName() + " " + p.getPrincipal().getMaturityYears())
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
			String accountNo = request.getAccountId();
			String accountType = request.getAccountType();
			List<String> targetProdIdList = request.getTargetProdIdList();
			List<SourceProdRatioDto> sourceProdList = request.getSourceProdList();
			List<BuyPlanFund> fundEntities = null;
			List<BuyPlanPG> pgEntities = null;
			
			if("DC".equals(accountType)) {
				fundEntities = buyPlanFundRepo.findByDcAccount_AccountNoAndFund_ProductIdIn(accountNo, targetProdIdList);
				pgEntities = buyPlanPGRepo.findByDcAccount_AccountNoAndPrincipal_ProductIdIn(accountNo, targetProdIdList); 
			} else {
				fundEntities = buyPlanFundRepo.findByIrpAccount_IrpAcctNoAndFund_ProductIdIn(accountNo, targetProdIdList);
				pgEntities = buyPlanPGRepo.findByIrpAccount_IrpAcctNoAndPrincipal_ProductIdIn(accountNo, targetProdIdList); 
			}
			 
			// 타겟 아이디 엔티티 상태값 N으로 수정
			if(fundEntities != null) {
				for(BuyPlanFund entity : fundEntities) {
					entity.setIsCurrent("N");
					buyPlanFundRepo.save(entity);
				}
			}
			

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
							.principal(PrincipalGuarantee.builder().productId(prodId).build())
							.allocationPercent(ratio)
							.isCurrent("Y")
							.build();
					if("DC".equals(accountType)) {
						DcAccount dcAccount = dcAccountRepository.findByAccountNo(accountNo);
						newEntity.setDcAccount(dcAccount);
					}
					else newEntity.setIrpAccount(IrpAccount.builder().irpAcctNo(accountNo).build());
					
					buyPlanPGRepo.save(newEntity);
				} else { //fund인 경우
					BuyPlanFund newEntity = BuyPlanFund.builder()
							.user(UserEntity.builder().userId(userId).build())
							.fund(FundMaster.builder().productId(prodId).build())
							.allocationPercent(ratio)
							.isCurrent("Y")
							.build();
					
					if("DC".equals(accountType)) {
						DcAccount dcAccount = dcAccountRepository.findByAccountNo(accountNo);
						newEntity.setDcAccount(dcAccount);
					}
					else newEntity.setIrpAccount(IrpAccount.builder().irpAcctNo(accountNo).build());
					
					buyPlanFundRepo.save(newEntity);
				}
			}
			return "매수상품 예정 등록이 완료되었습니다.";
		} catch (Exception e) {
			e.printStackTrace();
			return "매수 상품 예정 업데이트 중 오류 발생";
		}
	}
}
