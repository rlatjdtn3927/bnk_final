//package com.example.memo.rule.controller;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.SessionAttribute;
//
//import com.example.memo.rule.dto.AnswerDTO;
//import com.example.memo.rule.dto.ProfileResultDTO;
//
//import lombok.RequiredArgsConstructor;
//
//@RequiredArgsConstructor
//@Controller
//public class SurveyController {
//
//    private final Survey surveyService;
//
//    @GetMapping("/survey-view")
//    public String showSurveyPage() {
//        return "survey";
//    }
//
//    @PostMapping("/survey-view")
//    public String handleSurveySubmit(
//            @RequestParam Map<String, String> params,
//            @RequestParam(required = false, name = "q9") List<String> q9Values, // 체크박스
//            @SessionAttribute(value = "user", required = false) Long userId,   // 세션 userId 일원화
//            Model model) {
//
//        if (userId == null) {
//            return "redirect:/login-view";
//        }
//
//        // 1) 웹 폼 파라미터 → AnswerDTO 리스트로 매핑
//        List<AnswerDTO> answers = new ArrayList<>();
//
//        // q1~q11 중 q9 제외(단일 선택들)
//        for (int i = 1; i <= 11; i++) {
//            if (i == 9) continue;
//            String key = "q" + i;
//            if (params.containsKey(key)) {
//                AnswerDTO a = new AnswerDTO();
//                a.setId(key);
//                a.setValue(params.get(key));   // 라디오/단일
//                answers.add(a);
//            }
//        }
//
//        // q9(체크박스 복수선택)
//        if (q9Values != null && !q9Values.isEmpty()) {
//            AnswerDTO a9 = new AnswerDTO();
//            a9.setId("q9");
//            a9.setValues(q9Values);            // 체크박스/복수
//            answers.add(a9);
//        }
//
//        // 2) 점수 계산(공통 서비스)
//        int score = .calculateScore(answers);
//
//        // 3) 저장(서비스 내부에서 typeId 계산 + result/history 반영)
//        survey.saveProfileAnalysis(userId, score);
//
//        // 4) 화면 표시용 데이터
//        String typeName = survey.findTypeNameByScore(score);
//        model.addAttribute("score", score);
//        model.addAttribute("type", typeName);
//
//        return "survey-result";
//    }
//
//    @PostMapping("/analyze")
//    public ResponseEntity<String> analyzeResult(@RequestBody ProfileResultDTO dto) {
//        survey.saveProfileAnalysis(dto);
//        return ResponseEntity.ok("분석 결과 저장 완료");
//    }
//}
