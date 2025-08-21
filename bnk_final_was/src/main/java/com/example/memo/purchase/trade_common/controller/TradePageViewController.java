// src/main/java/com/example/memo/purchase/controller/TradePageController.java
package com.example.memo.purchase.trade_common.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.memo.purchase.trade_common.dto.ReserveValueDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/purchase/trade")
@RequiredArgsConstructor
public class TradePageViewController {
	
	private final TcpClientService tcp;
    private final ObjectMapper om;
	
    /** step1 페이지: (필요 시) IRP/DC 목록을 세션에 저장해서 화면에서 session.*로 조회 */
    /** step1: IRP/DC 조회 결과를 전부 세션에 저장해서 화면에서 ${session.*}로 사용 */
    @GetMapping("/step1")
    public String step1(HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) {
            return "redirect:/login-view/main";
        }

        // ★ 없으면 새로 만들어 세션에 넣어둠 (방어)
        if (session.getAttribute("ReserveValueDto") == null) {
            session.setAttribute("ReserveValueDto", new ReserveValueDto());
        }

        JsonNode req = om.createObjectNode().put("userId", userId);

        // IRP 1건
        JsonNode irpRes = tcp.sendMessage(new TcpMessage(Command.ACCOUNT_GET_IRP_BY_USER, req));
        String irpAcctNo = (irpRes != null && irpRes.hasNonNull("irpAcctNo"))
                ? irpRes.get("irpAcctNo").asText() : null;

        // DC N건
        JsonNode dcRes = tcp.sendMessage(new TcpMessage(Command.ACCOUNT_GET_DC_LIST_BY_USER, req));
        List<String> dcAccounts = new ArrayList<>();
        if (dcRes != null && dcRes.has("dcAccounts") && dcRes.get("dcAccounts").isArray()) {
            for (JsonNode n : dcRes.get("dcAccounts")) {
                if (n != null && !n.isNull()) dcAccounts.add(n.asText());
            }
        }

        session.setAttribute("irpAcctNo", irpAcctNo);
        session.setAttribute("dcAccounts", dcAccounts);
        session.setAttribute("hasIrp", irpAcctNo != null);
        session.setAttribute("hasDc", !dcAccounts.isEmpty());

        return "purchase/trade/step1";
    }

    /** 네가 요구한 형식 그대로: 세션의 "PassValueDto"만 사용 */
    @PostMapping("/step1/next")
    public String step1Next(@RequestParam("accountType") String accountType,
                            @RequestParam("accountId")   String accountId,
                            HttpSession session) {

        // ★ 세션에서 DTO를 꺼내고, 없으면 생성(방어)
        ReserveValueDto dto = (ReserveValueDto) session.getAttribute("ReserveValueDto");
        if (dto == null) dto = new ReserveValueDto();

        dto.setAccountType(accountType); // IRP | DC
        dto.setAccountId(accountId);     // irp_acct_no | account_no
        session.setAttribute("ReserveValueDto", dto);

        return "redirect:/survey-view";   // 투자성향분석 화면(타 팀 구현)
    }

    /** step2 진입 (세션만 사용; 뷰에서 ${session.PassValueDto.*}로 표시) */
    @GetMapping("/step1/after-risk")
    public String step2AfterRisk(HttpSession session) {
        return "purchase/trade/step2_reserve";
    }
	
	/*테스트용 함수*/
	@GetMapping("/dummy")
	public String dummy() {
		return "purchase/trade/step1_form";
	}
    
    /*step2에서 호출하는 함수 step4나 step5로 분기*/
    @PostMapping("/step2/next")
    public String step2Next(@RequestParam(required = false, name = "targetProdIdList") String targetProdIdListJson,
				            @RequestParam(required = false, name = "sourceProdIdList") String sourceProdIdListJson,
				            @RequestParam(required = false, name = "fileUrlList") String fileUrlListJson,
				            @RequestParam(required = false, name = "sourceProdId") String legacyPairsJson,
				            HttpSession session) {
    	ReserveValueDto dto = (ReserveValueDto) session.getAttribute("ReserveValueDto");
    	dto.setSourceProdIdList(sourceProdIdListJson);
    	dto.setTargetProdIdList(targetProdIdListJson);
    	dto.setFileUrlList(fileUrlListJson);
    	session.setAttribute("ReserveValueDto", dto);
    	
    	System.out.println("TARGETPRODIDLIST: " + targetProdIdListJson);
    	System.out.println("SOURCEPRODIDLIST: " + sourceProdIdListJson);
    	System.out.println("FILEURLLIST: " + fileUrlListJson);
    	System.out.println("sourceProdId: " + legacyPairsJson);
    	
    	if(fileUrlListJson.equals("[]")) {
    		return "purchase/trade/step5"; // 비교 view
    	} else {
    		 return "purchase/trade/step4"; //서류동의 view
    	}
    }

	//    세션 DTO 덮어쓰기 금지 → “머지 저장”으로 유지
	//    step3/next, step4/next에서 session.setAttribute("PassValueDto", dto)로 통째로 덮어쓰면 flow 같은 공통필드가 사라질 수 있어요.
	//    아래처럼 세션값을 꺼내서 해당 스텝 필드만 갱신하세요.
    @PostMapping("/step4/next")
    public String step4Next(ReserveValueDto incoming, HttpSession session) {
        ReserveValueDto dto = (ReserveValueDto) session.getAttribute("ReserveValueDto");
        if (dto == null) dto = new ReserveValueDto();

        if (incoming.getTargetProdIdList() != null) dto.setTargetProdIdList(incoming.getTargetProdIdList());
        if (incoming.getTargetProdId() != null)     dto.setTargetProdId(incoming.getTargetProdId());
        // 필요 시 동의 결과 등을 받는다면 여기서만 반영

        session.setAttribute("ReserveValueDto", dto);
        return "redirect:/purchase/trade/step5";
    }

    /** Step5 화면 진입 (세션만 읽음) */
    @GetMapping("/step5")
    public String step5Page(HttpSession session) {
        ReserveValueDto dto = (ReserveValueDto) session.getAttribute("ReserveValueDto");
        session.setAttribute("ReserveValueDto", dto);
        return "purchase/trade/step5";
    }
    
    @PostMapping("/step5/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
    public String step5Next() {
        return "redirect:purchase/trade/step6"; // 완료 리다이렉트
    }
    
    @GetMapping("/step6") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
    public String step6() {
        return "purchase/trade/step6"; // 완료 view
    }
}

