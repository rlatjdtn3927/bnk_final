package com.example.memo.purchase.trade_common.controller.change;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.memo.purchase.trade_common.dto.ChangeValueDto;
import com.example.memo.rule.dto.SurveySubmitRes;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/purchase/trade/change")
@RequiredArgsConstructor
public class ChangePageViewController {
	
	private final TcpClientService tcp;
	private final ObjectMapper om;
	
	@GetMapping("/step1")
	public String step1(HttpSession session) {
		Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
		if (userId == null)
			return "redirect:/login-view/main";

		// flow 방어: 엔트리를 안 거쳤다면 엔트리로
		ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
		if (dto == null || dto.getFlow() == null || dto.getFlow().isBlank()) {
			return "redirect:/purchase/trade/change";
		}

		// AP 호출
		JsonNode req = om.createObjectNode().put("userId", userId);

		// IRP 1건
		JsonNode irpRes = tcp.sendMessage(new TcpMessage(Command.ACCOUNT_GET_IRP_BY_USER, req));
		String irpAcctNo = (irpRes != null && irpRes.hasNonNull("irpAcctNo")) ? irpRes.get("irpAcctNo").asText() : null;

		// DC N건
		JsonNode dcRes = tcp.sendMessage(new TcpMessage(Command.ACCOUNT_GET_DC_LIST_BY_USER, req));
		List<String> dcAccounts = new ArrayList<>();
		if (dcRes != null && dcRes.has("dcAccounts") && dcRes.get("dcAccounts").isArray()) {
			for (JsonNode n : dcRes.get("dcAccounts")) {
				if (n != null && !n.isNull())
					dcAccounts.add(n.asText());
			}
		}

		// 세션 저장 (step1.html에서 ${session.*}로 바로 사용)
		session.setAttribute("irpAcctNo", irpAcctNo);
		session.setAttribute("dcAccounts", dcAccounts);
		session.setAttribute("hasIrp", irpAcctNo != null);
		session.setAttribute("hasDc", !dcAccounts.isEmpty());

		return "purchase/trade/change/step1";
	}

	/** 네가 요구한 형식 그대로: 세션의 "PassValueDto"만 사용 */
	@PostMapping("/step1/next")
	public String step1Next(@RequestParam("accountType") String accountType,
			@RequestParam("accountId") String accountId, HttpSession session) {
		Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
		if (userId == null)
			return "redirect:/login-view/main";

		ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");

		// 필수값 방어
		if (accountType == null || accountType.isBlank() || accountId == null || accountId.isBlank()) {
			// 계좌 미선택 시 다시 step1
			return "redirect:/purchase/trade/change/step1";
		}

		dto.setAccountType(accountType); // IRP | DC
		dto.setAccountId(accountId); // irp_acct_no | account_no
		session.setAttribute("ChangeValueDto", dto);

		return "redirect:/survey-view"; // 투자성향분석
	}

	/** step2 진입 (세션만 사용; 뷰에서 ${session.PassValueDto.*}로 표시) */
	@GetMapping("/step1/after-risk")
	public String step2AfterRisk(HttpSession session) {
		ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
		if (dto == null)
			dto = new ChangeValueDto();

		// 설문 컨트롤러가 세션에 넣어둔 결과 읽기 (SurveyResultController에서
		// session.setAttribute("result", result))
		SurveySubmitRes result = (SurveySubmitRes) session.getAttribute("result");
		if (result != null) {
			dto.setRiskGrade(result.getType()); // 예: "위험중립형"
			// dto.setRiskGradeNum( ... ) // 점수→등급 숫자 매핑 시
		}

		session.setAttribute("ChangeValueDto", dto);
		return "purchase/trade/change/step2_change"; // 바로 뷰
	}
}
