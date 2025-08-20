// WAS - src/main/java/com/example/memo/purchase/controller/management/rest_controller/Step4TestSeedController.java
package com.example.memo.purchase.controller.management.rest_controller;

import com.example.memo.purchase.dto.trade.FileUrlDto;
import com.example.memo.purchase.dto.trade.PassValueDto;
import com.example.memo.purchase.dto.trade.SourceProductDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class Step4TestSeedController {

    @GetMapping("/purchase/trade/test/seed-reserve")
    public RedirectView seedReserve(HttpSession session) {
        PassValueDto dto = new PassValueDto();
        dto.setFlow("RESERVE");
        dto.setAccountType("IRP");
        dto.setAccountId("123-45-678901");

        dto.setSourceProdIdList(List.of(
                Map.of(new SourceProductDto("PG0012345","BNK 안심채권 6M"), 60),
                Map.of(new SourceProductDto("FND00077","BNK 코어채권펀드"),   40)
        ));

        List<FileUrlDto> files = new ArrayList<>();
        files.add(FileUrlDto.builder().prodId("PG0012345").prodName("BNK 안심채권 6M")
                .docType("상품설명서").fileUrl("https://example.com/pg0012345/desc.pdf").build());
        files.add(FileUrlDto.builder().prodId("PG0012345").prodName("BNK 안심채권 6M")
                .docType("약관").fileUrl("https://example.com/pg0012345/terms.pdf").build());
        files.add(FileUrlDto.builder().prodId("FND00077").prodName("BNK 코어채권펀드")
                .docType("투자설명서").fileUrl("https://example.com/fnd00077/prospectus.pdf").build());
        files.add(FileUrlDto.builder().prodId("FND00077").prodName("BNK 코어채권펀드")
                .docType("간이투자설명서").fileUrl("https://example.com/fnd00077/keyfacts.pdf").build());
        files.add(FileUrlDto.builder().prodId("FND00077").prodName("BNK 코어채권펀드")
                .docType("상품약관").fileUrl("https://example.com/fnd00077/policy.pdf").build());

        dto.setFileUrlList(files);
        session.setAttribute("PassValueDto", dto);
        return new RedirectView("/purchase/trade/step4");
    }

    @GetMapping("/purchase/trade/test/seed-change")
    public RedirectView seedChange(HttpSession session) {
        PassValueDto dto = new PassValueDto();
        dto.setFlow("CHANGE");
        dto.setAccountType("IRP");
        dto.setAccountId("123-45-678901");
        dto.setTargetProdId("FND00077");   // 변경 전
        dto.setSourceProdId("PG0012345");  // 변경 후

        List<FileUrlDto> files = List.of(
                FileUrlDto.builder().prodId("FND00077").prodName("BNK 코어채권펀드")
                        .docType("투자설명서").fileUrl("https://example.com/fnd00077/prospectus.pdf").build(),
                FileUrlDto.builder().prodId("PG0012345").prodName("BNK 안심채권 6M")
                        .docType("상품설명서").fileUrl("https://example.com/pg0012345/desc.pdf").build()
        );
        dto.setFileUrlList(files);
        session.setAttribute("PassValueDto", dto);
        return new RedirectView("/purchase/trade/step4");
    }

    @GetMapping("/purchase/trade/test/seed-maturity")
    public RedirectView seedMaturity(HttpSession session) {
        PassValueDto dto = new PassValueDto();
        dto.setFlow("MATURITY");
        dto.setAccountType("DC");
        dto.setAccountId("DC-00001");
        dto.setTargetProdId("PG0099999");
        dto.setSourceProdId("FND00077");

        List<FileUrlDto> files = List.of(
                FileUrlDto.builder().prodId("PG0099999").prodName("BNK 원리금플러스 12M")
                        .docType("약관").fileUrl("https://example.com/pg0099999/terms.pdf").build(),
                FileUrlDto.builder().prodId("FND00077").prodName("BNK 코어채권펀드")
                        .docType("간이투자설명서").fileUrl("https://example.com/fnd00077/keyfacts.pdf").build()
        );
        dto.setFileUrlList(files);
        session.setAttribute("PassValueDto", dto);
        return new RedirectView("/purchase/trade/step4");
    }

    @GetMapping("/purchase/trade/test/clear")
    public String clear(HttpSession session){
        session.invalidate();
        return "OK";
    }
}
