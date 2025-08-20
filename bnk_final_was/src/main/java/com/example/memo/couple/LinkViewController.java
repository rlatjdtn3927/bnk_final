package com.example.memo.couple;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/link-view")
public class LinkViewController {

    /** 부부연동 신청 화면 */
    @GetMapping("/apply")
    public String applyForm(HttpSession session, Model model) {
        // 세션 사용자 표시(없어도 화면은 열림)
        Object user = session.getAttribute("user");
        model.addAttribute("sessionUserId", user);
        return "couple/apply";  // templates/link/apply.html
    }

    /** (선택) 간단 메인 이동용 */
    @GetMapping("/main")
    public String main() {
        return "couple/main"; // 필요 없으면 제거하거나 apply로 redirect
    }
}