// WAS - src/main/java/com/example/memo/purchase/controller/management/rest_controller/Step4TestSeedController.java
package com.example.memo.purchase.controller.management.rest_controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.example.memo.purchase.dto.trade.FileUrlDto;
import com.example.memo.purchase.dto.trade.PassValueDto;
import com.example.memo.purchase.dto.trade.SourceProductDto;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class Step4TestSeedController {

    /** 1) RESERVE (여러 상품 + 비율) */
    @GetMapping("/purchase/trade/test/seed-reserve")
    public RedirectView seedReserve(HttpSession session) {
        PassValueDto dto = new PassValueDto();
        dto.setFlow("RESERVE");
        dto.setAccountType("IRP");
        dto.setAccountId("123-45-678901");

        // 선택 상품(비율) 예시: PG0012345 60% + FND00077 40%
        dto.setSourceProdIdList(List.of(
        	    Map.of(SourceProductDto.builder()
        	                           .productId("PG0012345")
        	                           .productName("상품A").build(), 60),
        	    Map.of(SourceProductDto.builder()
        	                           .productId("FND00077")
        	                           .productName("펀드77").build(), 40)
        	));

        // 문서 목록(세션만 사용하므로 여기서 넣어줘야 화면에 뜬다)
        List<FileUrlDto> files = new ArrayList<>();
        // PG(원리금보장)
        files.add(FileUrlDto.builder()
                .prodId("PG0012345").prodName("BNK 안심채권 6M")
                .docType("상품설명서").fileUrl("https://example.com/pg0012345/desc.pdf").build());
        files.add(FileUrlDto.builder()
                .prodId("PG0012345").prodName("BNK 안심채권 6M")
                .docType("약관").fileUrl("https://example.com/pg0012345/terms.pdf").build());
        // FND(펀드/ETF/TDF)
        files.add(FileUrlDto.builder()
                .prodId("FND00077").prodName("BNK 코어채권펀드")
                .docType("투자설명서").fileUrl("https://example.com/fnd00077/prospectus.pdf").build());
        files.add(FileUrlDto.builder()
                .prodId("FND00077").prodName("BNK 코어채권펀드")
                .docType("간이투자설명서").fileUrl("https://example.com/fnd00077/keyfacts.pdf").build());
        files.add(FileUrlDto.builder()
                .prodId("FND00077").prodName("BNK 코어채권펀드")
                .docType("상품약관").fileUrl("https://example.com/fnd00077/policy.pdf").build());

        dto.setFileUrlList(files);
        session.setAttribute("PassValueDto", dto);
        return new RedirectView("/purchase/trade/step4");
    }

    /** 2) CHANGE (단일 상품) */
    @GetMapping("/purchase/trade/test/seed-change")
    public RedirectView seedChange(HttpSession session) {
        PassValueDto dto = new PassValueDto();
        dto.setFlow("CHANGE");
        dto.setAccountType("IRP");
        dto.setAccountId("123-45-678901");
        dto.setSourceProdId("FND00077");

        List<FileUrlDto> files = List.of(
                FileUrlDto.builder().prodId("FND00077").prodName("BNK 코어채권펀드")
                        .docType("투자설명서").fileUrl("https://example.com/fnd00077/prospectus.pdf").build(),
                FileUrlDto.builder().prodId("FND00077").prodName("BNK 코어채권펀드")
                        .docType("간이투자설명서").fileUrl("https://example.com/fnd00077/keyfacts.pdf").build()
        );
        dto.setFileUrlList(files);
        session.setAttribute("PassValueDto", dto);
        return new RedirectView("/purchase/trade/step4");
    }

    /** 3) MATURITY (단일 상품) */
    @GetMapping("/purchase/trade/test/seed-maturity")
    public RedirectView seedMaturity(HttpSession session) {
        PassValueDto dto = new PassValueDto();
        dto.setFlow("MATURITY");
        dto.setAccountType("DC");
        dto.setAccountId("DC-00001");
        dto.setSourceProdId("PG0099999");

        List<FileUrlDto> files = List.of(
                FileUrlDto.builder().prodId("PG0099999").prodName("BNK 원리금플러스 12M")
                        .docType("상품설명서").fileUrl("https://example.com/pg0099999/desc.pdf").build(),
                FileUrlDto.builder().prodId("PG0099999").prodName("BNK 원리금플러스 12M")
                        .docType("약관").fileUrl("https://example.com/pg0099999/terms.pdf").build()
        );
        dto.setFileUrlList(files);
        session.setAttribute("PassValueDto", dto);
        return new RedirectView("/purchase/trade/step4");
    }

    /** 세션 초기화(테스트 편의) */
    @GetMapping("/purchase/trade/test/clear")
    public String clear(HttpSession session){
        session.invalidate();
        return "OK";
    }
}
