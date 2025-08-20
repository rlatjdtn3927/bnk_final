package com.example.memo.couple.service;

import java.time.LocalDate;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.example.memo.couple.ocr.dto.SpouseOcrRequest;
import com.example.memo.couple.ocr.dto.SpouseOcrResponse;
import com.example.memo.couple.ocr.service.ClovaSpouseOcrService;
import com.example.memo.jpa.entity.couple.IrpSpouseLink;
import com.example.memo.jpa.entity.couple.LinkAdminReview;
import com.example.memo.jpa.entity.couple.LinkStatus;
import com.example.memo.jpa.entity.couple.ReviewStatus;
import com.example.memo.jpa.entity.user.UserEntity;
import com.example.memo.jpa.repository.couple.IrpSpouseLinkRepository;
import com.example.memo.jpa.repository.couple.LinkAdminReviewRepository;
import com.example.memo.jpa.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import static com.example.memo.couple.ocr.OcrTextUtil.*;


@Service
@RequiredArgsConstructor
public class SpouseLinkService {

    private final IrpSpouseLinkRepository linkRepo;
    private final LinkAdminReviewRepository reviewRepo;
    private final UserRepository userRepo;
    private final S3Service s3Service;
    private final ClovaSpouseOcrService spouseOcrService;
    private final ObjectMapper om;

    @Transactional
    public ObjectNode apply(Long applicantUserId,
                            String spouseName,
                            String spouseBirth,           // yyyy-MM-dd 기대(자유형식도 허용)
                            String filename,
                            String contentType,
                            String base64) {

        if (applicantUserId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (linkRepo.existsActiveForUser(applicantUserId)) {
            throw new IllegalStateException("진행 중이거나 연동된 내역이 있습니다.");
        }

        // 본인 정보 조회
        UserEntity me = userRepo.findById(applicantUserId)
                .orElseThrow(() -> new IllegalStateException("사용자 정보를 찾을 수 없습니다."));

        // 신청 행 생성(APPLIED)
        IrpSpouseLink link = new IrpSpouseLink();
        link.setApplicantUserId(applicantUserId);
        link.setLinkStatus(LinkStatus.APPLIED);
        link.setAppliedAt(LocalDate.now());
        link = linkRepo.save(link);

        boolean ocrOk = false;
        String failReason = null;
        SpouseOcrResponse rs = null;

        if (base64 != null && !base64.isBlank()) {
            try {
                SpouseOcrRequest rq = new SpouseOcrRequest();
                rq.setBase64(base64);
                rq.setFilename(filename);
                rq.setContentType(contentType);

                rs = spouseOcrService.parse(rq);

                if (rs != null && rs.isOk() && rs.isSpouseRelation()) {
                    // 1) 신청자 DB ↔ OCR
                    boolean selfNameOk  = safeEqualsName(me.getName(), rs.getApplicantName());
                    boolean selfBirthOk = sameBirth(dateToYmd(me.getBirthDate()), rs.getApplicantBirth());

                    // 2) 배우자 입력 ↔ OCR
                    boolean spouseNameOk  = safeEqualsName(spouseName, rs.getSpouseName());
                    boolean spouseBirthOk = sameBirth(spouseBirth, rs.getSpouseBirth());

                    ocrOk = selfNameOk && selfBirthOk && spouseNameOk && spouseBirthOk;

                    if (!ocrOk) {
                        StringBuilder sb = new StringBuilder();
                        if (!selfNameOk)   sb.append("본인 성명 불일치; ");
                        if (!selfBirthOk)  sb.append("본인 생년월일 불일치; ");
                        if (!spouseNameOk) sb.append("배우자 성명 불일치; ");
                        if (!spouseBirthOk)sb.append("배우자 생년월일 불일치; ");
                        failReason = sb.toString().replaceAll("; $", "");
                    }
                } else {
                    failReason = (rs != null && rs.getReason() != null)
                            ? rs.getReason()
                            : "OCR 실패 또는 배우자 정보 미검출";
                }
            } catch (Exception e) {
                failReason = "OCR 오류: " + e.getMessage();
            }
        } else {
            failReason = "증빙 미첨부";
        }

        // 공통 응답
        ObjectNode out = om.createObjectNode().put("linkId", link.getId());

        // OCR 결과 포함(뷰에서 바로 사용)
        ObjectNode ocr = om.createObjectNode();
        if (rs != null) {
            ocr.put("ok", rs.isOk());
            ocr.put("reason", rs.getReason());
            ocr.put("docType", rs.getDocType());
            ocr.put("applicantName", rs.getApplicantName());
            ocr.put("applicantBirth", rs.getApplicantBirth());
            ocr.put("spouseName", rs.getSpouseName());
            ocr.put("spouseBirth", rs.getSpouseBirth());
            ocr.put("spouseRelation", rs.isSpouseRelation());
        } else {
            ocr.put("ok", false);
            ocr.put("reason", failReason);
        }
        out.set("ocr", ocr);

        if (ocrOk) {
            // 배우자 수락 대기
            link.setLinkStatus(LinkStatus.PENDING_SPOUSE);
            linkRepo.save(link);

            out.put("status", LinkStatus.PENDING_SPOUSE.name());
            out.put("nextAction", "WAIT_SPOUSE");
            out.put("message", "배우자 수락 대기");
        } else {
            // 관리자 심사 대기 + 증빙 S3 보관
            if (base64 != null && !base64.isBlank()) {
                byte[] bytes = Base64.getDecoder().decode(base64);
                String key = s3Service.put("family-doc/", filename, bytes);
                link.setS3ObjectKey(key);
                out.put("s3ObjectKey", key);
            }
            link.setLinkStatus(LinkStatus.PENDING_ADMIN);
            linkRepo.save(link);

            LinkAdminReview rv = new LinkAdminReview();
            rv.setLinkId(link.getId());
            rv.setReviewStatus(ReviewStatus.PENDING);
            rv.setReason(failReason != null ? failReason : "OCR 불일치/실패");
            rv.setAdminMemo(failReason); // 상세 원문은 메모로
            rv.setRequestedAt(LocalDate.now());
            reviewRepo.save(rv);

            out.put("status", LinkStatus.PENDING_ADMIN.name());
            out.put("nextAction", "WAIT_ADMIN");
            out.put("message", "관리자 검토 대기");
            out.put("reviewReason", failReason);
        }

        return out;
    }

    /* ================= helpers ================= */

    private String dateToYmd(LocalDate d) {
        if (d == null) return null;
        return String.format("%04d-%02d-%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    /** 이름 비교: OcrTextUtil.normalizeNameForCompare 사용 */
    private boolean safeEqualsName(String a, String b) {
        if (a == null || b == null) return false;
        String na = normalizeNameForCompare(a);
        String nb = normalizeNameForCompare(b);
        return na != null && na.equals(nb);
    }

    /** 생일 비교: OcrTextUtil.normalizeBirth 사용 (완전 일치 기준) */
    private boolean sameBirth(String a, String b) {
        if (a == null || b == null) return false;
        String da = (a.contains("-") ? a : normalizeBirth(a));
        String db = (b.contains("-") ? b : normalizeBirth(b));
        if (da == null || db == null) return false;
        da = da.replaceAll("\\D", "");
        db = db.replaceAll("\\D", "");
        return da.length() == 8 && da.equals(db);
    }
}