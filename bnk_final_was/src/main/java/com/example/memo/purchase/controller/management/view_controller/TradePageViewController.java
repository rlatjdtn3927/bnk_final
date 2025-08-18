// src/main/java/com/example/memo/purchase/controller/TradePageController.java
package com.example.memo.purchase.controller.management.view_controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.memo.purchase.dto.ModelValueDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/purchase/trade")
@RequiredArgsConstructor
public class TradePageViewController {
	
	@GetMapping("/dummy")
	public String dummy() {
		return "purchase/trade/step1_form";
	}


    private final ObjectMapper objectMapper = new ObjectMapper();

    /** step2 화면 진입 (이전 단계에서 넘겨준 모델을 그대로 뷰에 바인딩) */
    @PostMapping("/step1/after-risk")
    public String step2After(@ModelAttribute ModelValueDto dto, Model model) {
        // 실서비스에선 검증/조회 등을 하겠지만, 여기서는 그대로 내려줌
        model.addAttribute("ModelValueDto", dto);
        return "purchase/trade/step2_reserve"; // templates/purchase/trade/step2.html
    }

    /** step3 제출 더미 – 화면 이동만 확인용 */
    @PostMapping("/step3/next")
    @ResponseBody
    public String step3Next(
            @RequestParam String flow,
            @RequestParam String accountType,
            @RequestParam String accountId,
            @RequestParam(required = false) String riskGrade,
            @RequestParam("sourceProdId") String sourceProdIdJson // ["K123:30","K456:70"] 형태(JSON 문자열)
    ) throws Exception {

        // 디버그: JSON 문자열 -> 자바 리스트 파싱
        List<String> items = objectMapper.readValue(sourceProdIdJson, new TypeReference<List<String>>() {});
        return """
               <html><body>
               <h3>STEP3 도착(더미)</h3>
               <ul>
                 <li>flow: %s</li>
                 <li>accountType: %s</li>
                 <li>accountId: %s</li>
                 <li>riskGrade: %s</li>
                 <li>sourceProdId(list): %s</li>
               </ul>
               <p>실서비스에서는 여기서 검증/저장 후 실제 step3 템플릿을 리턴하세요.</p>
               </body></html>
               """.formatted(flow, accountType, accountId, String.valueOf(riskGrade), items);
    }
	
}









//public class TradePageViewController {
//	
//	@GetMapping("/dummy")
//	public String dummy() {
//		return "purchase/trade/step1_form";
//	}
//		
//    @GetMapping("/step1") //model: flow
//    public String step1(Model model, RedirectAttributes rttr) {
//    		ModelValueDto dto = (ModelValueDto)rttr.getAttribute("ModelValueDto"); 
//    		model.addAttribute("ModelValueDto", dto);
//        return "purchase/trade/step1"; //계좌선택 view
//    }
//
//    @PostMapping("/step1/next") //model: flow, 계좌 id, type 
//    public String step1Next(ModelValueDto dto, Model model) {
//    		model.addAttribute("ModelValueDto", dto);
//    		if(dto.getFlow().equals("RESERVE")) {
//    			 return ""; // 투자성향 분석으로 가기
//    		} else {
//    			 return "purchase/trade/step2_others"; // 보유상품목록 view
//    		}
//    }
//    
//    /**************************임시**********************************/
//    @PostMapping("/step2/after-risk")
//    public String step2After(ModelValueDto dto, Model model) {
//		model.addAttribute("ModelValueDto", dto);	 
//	    return "purchase/trade/step2_reserve"; // 보유상품목록 view
//    }
//    /****************************임시********************************/
//    
//    //purchase/trade/step2_reserve뷰로 가는 함수만들기
//    
//    @PostMapping("/step2/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID
//    public String step2Next(ModelValueDto dto, Model model) {
//    		model.addAttribute("ModelValueDto", dto);
//        return "purchase/trade/step3"; // 상품선택목록 view
//    }
//    
//    @PostMapping("/step3/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
//    public String step3Next(ModelValueDto dto, Model model) {
//    		model.addAttribute("ModelValueDto", dto);
//        return "purchase/trade/step4"; // 서류 view
//    }
//    
//    @PostMapping("/step4/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
//    public String step4Next(ModelValueDto dto, Model model) {
//    		model.addAttribute("ModelValueDto", dto);
//        return "purchase/trade/step5"; // 비교확인 view
//    }
//    
//    @PostMapping("/step5/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
//    public String step5Next() {
//        return "redirect:purchase/trade/step6"; // 완료 리다이렉트
//    }
//    
//    @GetMapping("/step6") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
//    public String step6() {
//        return "purchase/trade/step6"; // 완료 view
//    }
//}

