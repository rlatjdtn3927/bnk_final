package com.example.memo.couple.service;

import static com.example.memo.couple.ocr.OcrTextUtil.normalizeBirth;
import static com.example.memo.couple.ocr.OcrTextUtil.normalizeNameForCompare;

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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    
    public ArrayNode adminListPending(int urlTtlMinutes) {
        log.info(">>>> [START] 관리자 검토 대기 목록 조회를 시작합니다.");

        var links = linkRepo.findByLinkStatusOrderByAppliedAtDesc(LinkStatus.PENDING_ADMIN);
        log.info(">>>> [DB RESULT] irp_spouse_link 테이블에서 PENDING_ADMIN 상태인 데이터를 {}건 찾았습니다.", links.size());

        if (links.isEmpty()) {
            log.warn(">>>> [END] 조회된 데이터가 없어 빈 목록을 반환합니다. DB에 PENDING_ADMIN 상태의 데이터가 있는지, 있다면 COMMIT이 되었는지 확인해주세요.");
            return om.createArrayNode();
        }

        ArrayNode arr = om.createArrayNode();
        for (IrpSpouseLink l : links) {
            ObjectNode o = om.createObjectNode();
            o.put("linkId", l.getId());
            o.put("applicantUserId", l.getApplicantUserId());
            o.put("status", l.getLinkStatus().name());
            o.put("appliedAt", l.getAppliedAt() != null ? l.getAppliedAt().toString() : null);

            reviewRepo.findTopByLinkIdOrderByIdDesc(l.getId()).ifPresent(rv -> {
                o.put("reviewReason", rv.getReason());
                o.put("requestedAt", rv.getRequestedAt() != null ? rv.getRequestedAt().toString() : null);
            });

            // 증빙 파일 URL 생성 (개별 예외 처리 강화)
            if (l.getS3ObjectKey() != null && !l.getS3ObjectKey().isBlank()) {
                try {
                    String url = s3Service.presignedGetUrl(
                            l.getS3ObjectKey(), java.time.Duration.ofMinutes(urlTtlMinutes));
                    o.put("proofUrl", url);
                    log.info(">>>> linkId={}의 증빙 파일 URL 생성 성공 (Key: {})", l.getId(), l.getS3ObjectKey());
                } catch (Exception e) {
                    log.error(">>>> [S3 ERROR] linkId={}의 증빙 파일 URL 생성 중 S3 오류 발생! (Key: {}). AWS 자격 증명(Credentials) 설정을 확인해주세요.", l.getId(), l.getS3ObjectKey(), e);
                    o.put("proofUrl", "#"); // 에러가 나도 링크는 '#'로 표시
                    o.put("s3Error", e.getMessage()); // 프론트에서 확인할 수 있도록 에러 메시지 추가
                }
            } else {
                o.putNull("proofUrl");
            }
            arr.add(o);
        }
        log.info(">>>> [END] 최종 {}건의 데이터를 JSON으로 변환하여 반환합니다.", arr.size());
        return arr;
    }

    @Transactional
    public ObjectNode adminDecide(long linkId, String action, String reason) {
        if (action == null || !(action.equalsIgnoreCase("APPROVE") || action.equalsIgnoreCase("REJECT"))) {
            throw new IllegalArgumentException("action은 APPROVE 또는 REJECT 이어야 합니다.");
        }

        IrpSpouseLink link = linkRepo.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("연동 신청이 존재하지 않습니다."));

        LinkAdminReview rv = reviewRepo.findTopByLinkIdOrderByIdDesc(linkId)
                .orElseThrow(() -> new IllegalStateException("검토 요청 내역이 없습니다."));

        if (rv.getReviewStatus() != ReviewStatus.PENDING) {
            throw new IllegalStateException("이미 심사 완료된 건입니다.");
        }

        if (action.equalsIgnoreCase("APPROVE")) {
            link.setLinkStatus(LinkStatus.PENDING_SPOUSE); // 배우자 수락 대기
            linkRepo.save(link);

            rv.setReviewStatus(ReviewStatus.APPROVED);
            rv.setCompletedAt(LocalDate.now());
            if (reason != null) rv.setAdminMemo(reason);
            reviewRepo.save(rv);
        } else { // REJECT
            link.setLinkStatus(LinkStatus.REJECTED_ADMIN);
            linkRepo.save(link);

            rv.setReviewStatus(ReviewStatus.REJECTED);
            rv.setCompletedAt(LocalDate.now());
            rv.setReason((reason != null && !reason.isBlank()) ? reason : "관리자 반려");
            rv.setAdminMemo(reason);
            reviewRepo.save(rv);
        }

        ObjectNode out = om.createObjectNode();
        out.put("linkId", link.getId());
        out.put("status", link.getLinkStatus().name());
        out.put("reviewStatus", rv.getReviewStatus().name());
        out.put("completedAt", rv.getCompletedAt() != null ? rv.getCompletedAt().toString() : null);
        out.put("reason", rv.getReason());
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