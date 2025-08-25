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
    
    /**
     * 배우자 연동 수락/거절 페이지
     * 알림 메시지를 클릭하면 이 주소로 이동
     */
    @GetMapping("/pending")
    public String spouseLinkPendingPage(HttpSession session, Model model) {
        // 이 페이지에 필요한 정보(예: 누가 신청했는지)를
        // AP 서버에서 조회하여 Model에 담아 전달하는 로직이 추가될 수 있습니다.
        Object user = session.getAttribute("user");
        model.addAttribute("sessionUserId", user);
        return "couple/pending"; // templates/couple/pending.html
    }
    
    /**
     * 전체 알림 목록 페이지
     */
    @GetMapping("/notifications")
    public String allNotificationsPage() {
        return "couple/notifications"; // templates/couple/notifications.html
    }


    @GetMapping("/main")
    public String main() {
        return "couple/couple-irp";
    }
}