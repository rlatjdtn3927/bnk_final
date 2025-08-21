package com.example.memo.couple.service;

import static com.example.memo.couple.ocr.OcrTextUtil.normalizeBirth;
import static com.example.memo.couple.ocr.OcrTextUtil.normalizeNameForCompare;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.example.memo.couple.ocr.dto.SpouseOcrRequest;
import com.example.memo.couple.ocr.dto.SpouseOcrResponse;
import com.example.memo.couple.ocr.service.ClovaSpouseOcrService;
import com.example.memo.jpa.entity.couple.IrpSpouseLink;
import com.example.memo.jpa.entity.couple.LinkAdminReview;
import com.example.memo.jpa.entity.couple.LinkStatus;
import com.example.memo.jpa.entity.couple.Notification;
import com.example.memo.jpa.entity.couple.ReviewStatus;
import com.example.memo.jpa.entity.user.UserEntity;
import com.example.memo.jpa.repository.couple.IrpSpouseLinkRepository;
import com.example.memo.jpa.repository.couple.LinkAdminReviewRepository;
import com.example.memo.jpa.repository.couple.NotificationRepository;
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

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepo;
    private final SseService sseService;

    @Transactional
    public ObjectNode apply(Long applicantUserId,
                            String spouseName,
                            String spouseBirth,
                            String filename,
                            String contentType,
                            String base64,
                            Long existingLinkId) {

        if (applicantUserId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        IrpSpouseLink link;

        if (existingLinkId != null) {
            // --- 재시도 ---
            link = linkRepo.findById(existingLinkId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));

            if (link.getOcrAttemptCount() >= 3) {
                throw new IllegalStateException("최대 3회까지만 시도할 수 있습니다. 관리자 심사를 요청해주세요.");
            }

            // 재시도마다 이전에 올려둔 S3 증빙은 존재 자체가 없어야 정책상 일관 → 혹시 있을 경우 제거
            if (link.getS3ObjectKey() != null) {
                s3Service.delete(link.getS3ObjectKey());
                link.setS3ObjectKey(null);
            }

        } else {
            link = linkRepo.findLatestOpenByApplicant(applicantUserId).orElse(null);
            if (link == null) {
                // 이미 LINKED로 묶인 계정이면 신규 신청 차단
                if (linkRepo.existsLinkedForUser(applicantUserId)) {
                    throw new IllegalStateException("이미 연동 완료된 내역이 있습니다.");
                }
                // 진짜 신규 생성
                link = new IrpSpouseLink();
                link.setApplicantUserId(applicantUserId);
                link.setLinkStatus(LinkStatus.APPLIED);
                link.setAppliedAt(LocalDate.now());
                link.setOcrAttemptCount(0);
                linkRepo.save(link);
            }
        }

        UserEntity me = userRepo.findById(applicantUserId)
                .orElseThrow(() -> new IllegalStateException("사용자 정보를 찾을 수 없습니다."));

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
                    boolean selfNameOk   = safeEqualsName(me.getName(), rs.getApplicantName());
                    boolean selfBirthOk  = sameBirth(dateToYmd(me.getBirthDate()), rs.getApplicantBirth());
                    boolean spouseNameOk = safeEqualsName(spouseName, rs.getSpouseName());
                    boolean spouseBirthOk= sameBirth(spouseBirth, rs.getSpouseBirth());

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
                    failReason = (rs != null && rs.getReason() != null) ? rs.getReason() : "OCR 실패 또는 배우자 정보 미검출";
                }
            } catch (Exception e) {
                failReason = "OCR 오류: " + e.getMessage();
            }
        } else {
            failReason = "증빙 미첨부";
        }

        // 시도 횟수 +1 (성공/실패 모두 금번 시도 반영)
        int currentCount = link.getOcrAttemptCount();
        link.setOcrAttemptCount(currentCount + 1);
        
        ObjectNode out = om.createObjectNode().put("linkId", link.getId());
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
            // 배우자 가입자 매칭
            String spouseNameOcr = rs.getSpouseName();
            LocalDate spouseBirthOcr = LocalDate.parse(rs.getSpouseBirth(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            UserEntity spouseUser = userRepo.findByNameAndBirthDate(spouseNameOcr, spouseBirthOcr)
                    .orElseThrow(() -> new IllegalStateException("연동하려는 배우자가 서비스에 가입되어 있지 않습니다."));

            link.setSpouseUserId(spouseUser.getUserId());
            link.setLinkStatus(LinkStatus.PENDING_SPOUSE);
            linkRepo.save(link);

            // 알림
            UserEntity applicantUser = me;
            String message = applicantUser.getName() + "님이 부부 IRP 연동을 신청했습니다.";

            notificationService.createNotification(
                spouseUser.getUserId(),
                message,
                "/link-view/pending",
                link.getId()
            );

            // 실시간 SSE 발송
            sseService.send(spouseUser.getUserId(), "new_link_request", message);


            out.put("status", LinkStatus.PENDING_SPOUSE.name());
            out.put("nextAction", "WAIT_SPOUSE");
            out.put("message", "배우자 수락 대기");

        } else {
            // 🔴 여기서 더 이상 S3 업로드 하지 않음! (정책: 관리 심사 요청 때만 업로드)
            // 실패 후 분기
            if (link.getOcrAttemptCount() >= 3) {
                out.put("status", link.getLinkStatus().name()); // APPLIED 그대로
                out.put("nextAction", "AWAIT_ADMIN_REQUEST");
                out.put("message", "자동 판독에 3회 실패했습니다. 관리자에게 직접 검토를 요청할 수 있습니다.");
            } else {
                out.put("status", link.getLinkStatus().name());
                out.put("nextAction", "RETRY_OCR");
                out.put("message", String.format("OCR 판독 실패 (%d/3회). 선명한 사진으로 다시 시도해주세요.", link.getOcrAttemptCount()));
            }
            linkRepo.save(link);
        }

        return out;
    }

    
    /**
     * 관리자가 검토 대기중인 연동 신청 목록을 조회
     * (배우자 정보 조회 로직 추가)
     */
    public ArrayNode adminListPending(int urlTtlMinutes) {
        log.info(">>>> [START] 관리자 검토 대기 목록 조회를 시작합니다.");

        var links = linkRepo.findByLinkStatusOrderByAppliedAtDesc(LinkStatus.PENDING_ADMIN);
        log.info(">>>> [DB RESULT] irp_spouse_link 테이블에서 PENDING_ADMIN 상태인 데이터를 {}건 찾았습니다.", links.size());

        if (links.isEmpty()) {
            log.warn(">>>> [END] 조회된 데이터가 없어 빈 목록을 반환합니다.");
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
            
            // spouse_user_id가 있다면, 해당 사용자의 정보를 찾아서 함께 내려줌
            if (l.getSpouseUserId() != null) {
                userRepo.findById(l.getSpouseUserId()).ifPresent(spouse -> {
                    o.put("spouseUserId", spouse.getUserId());
                    o.put("spouseName", spouse.getName());
                });
            }

            // 증빙 파일 URL 생성 (기존 로직과 동일)
            if (l.getS3ObjectKey() != null && !l.getS3ObjectKey().isBlank()) {
                try {
                    String url = s3Service.presignedGetUrl(
                            l.getS3ObjectKey(), java.time.Duration.ofMinutes(urlTtlMinutes));
                    o.put("proofUrl", url);
                } catch (Exception e) {
                    log.error(">>>> [S3 ERROR] linkId={} 증빙 파일 URL 생성 오류!", l.getId(), e);
                    o.put("proofUrl", "#");
                    o.put("s3Error", e.getMessage());
                }
            } else {
                o.putNull("proofUrl");
            }
            arr.add(o);
        }
        log.info(">>>> [END] 최종 {}건의 데이터를 JSON으로 변환하여 반환합니다.", arr.size());
        return arr;
    }

    /**
     * 관리자가 연동 신청을 승인하거나 반려
     * @param linkId 처리할 연동 신청 ID
     * @param spouseUserId 관리자가 확정한 배우자의 ID (승인 시에만 필요)
     * @param action "APPROVE" 또는 "REJECT"
     * @param reason 반려 사유 또는 관리자 메모
     * @return 처리 결과가 담긴 ObjectNode
     */
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
        
        Long applicantUserId = link.getApplicantUserId();
        String message;

        if (action.equalsIgnoreCase("APPROVE")) {
            Long spouseUserId = link.getSpouseUserId(); // <-- DB에 저장된 값을 직접 사용
            if (spouseUserId == null) {
                // 이 경우는 requestAdminReview 로직에 문제가 있다는 뜻
                throw new IllegalStateException("승인 처리할 배우자가 지정되어 있지 않습니다.");
            }
            
            link.setLinkStatus(LinkStatus.PENDING_SPOUSE);
            rv.setReviewStatus(ReviewStatus.APPROVED);
            if (reason != null) rv.setAdminMemo(reason);

            // 1. 배우자에게 보낼 알림
            UserEntity applicant = userRepo.findById(applicantUserId)
                                           .orElseThrow(() -> new IllegalStateException("신청자 정보를 찾을 수 없습니다."));
            message = applicant.getName() + "님이 부부 IRP 연동을 신청했습니다. 수락해주세요.";
            notificationService.createNotification(spouseUserId, message, "/link-view/pending", linkId);
            sseService.send(spouseUserId, "admin_approved_for_spouse", message);

            // 2. 신청자에게 보낼 '서류 심사 통과' 알림
            String messageToApplicant = "관리자가 서류를 확인하고 연동 신청을 승인했습니다. 배우자의 최종 수락을 기다려주세요.";
            notificationService.createNotification(applicantUserId, messageToApplicant, null, linkId);
            sseService.send(applicantUserId, "admin_approved_for_applicant", messageToApplicant);

        } else { // REJECT
            link.setLinkStatus(LinkStatus.REJECTED_ADMIN);
            rv.setReviewStatus(ReviewStatus.REJECTED);

            String rejectionReason = (reason != null && !reason.isBlank()) ? reason : "관리자 반려";
            rv.setReason(rejectionReason);
            rv.setAdminMemo(reason);

            message = "관리자가 부부 IRP 연동 신청을 반려했습니다. 사유: " + rejectionReason;
            notificationService.createNotification(applicantUserId, message, "/link-view/apply", linkId);
            sseService.send(applicantUserId, "admin_rejected", message);
        }

        linkRepo.save(link);
        rv.setCompletedAt(LocalDate.now());
        reviewRepo.save(rv);

        ObjectNode out = om.createObjectNode();
        out.put("linkId", link.getId());
        out.put("status", link.getLinkStatus().name());
        out.put("reviewStatus", rv.getReviewStatus().name());
        return out;
    }

    
    /**
     * 배우자가 연동 요청을 수락하거나 거절
     * @param linkId 처리할 연동 요청 ID
     * @param spouseUserId 결정을 내리는 사용자(배우자)의 ID
     * @param action "ACCEPT" 또는 "REJECT"
     * @return 처리 결과가 담긴 ObjectNode
     */
    @Transactional
    public ObjectNode decideBySpouse(Long linkId, Long spouseUserId, String action) {
        if (!"ACCEPT".equalsIgnoreCase(action) && !"REJECT".equalsIgnoreCase(action)) {
            throw new IllegalArgumentException("action은 'ACCEPT' 또는 'REJECT' 이어야 합니다.");
        }

        IrpSpouseLink link = linkRepo.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 연동 신청입니다."));

        // 이 요청을 보낸 사용자가 실제 연동 대상 배우자가 맞는지 확인
        if (!spouseUserId.equals(link.getSpouseUserId())) {
            throw new SecurityException("연동 대상 배우자 본인만 처리할 수 있습니다.");
        }
        
        // 현재 상태가 '배우자 수락 대기' 상태인지 확인
        if (link.getLinkStatus() != LinkStatus.PENDING_SPOUSE) {
            throw new IllegalStateException("이미 처리되었거나 처리할 수 없는 상태입니다.");
        }
        
        Long applicantUserId = link.getApplicantUserId();
        UserEntity spouseUser = userRepo.findById(spouseUserId)
                                        .orElseThrow(() -> new IllegalStateException("배우자 정보를 찾을 수 없습니다."));
        String messageToApplicant;

        if ("ACCEPT".equalsIgnoreCase(action)) {
            link.setLinkStatus(LinkStatus.LINKED); // 상태를 '연동 완료'로 변경
            link.setApprovedAt(LocalDate.now());   // 연동 완료일 기록

            // 신청자에게 보낼 '연동 완료' 알림
            messageToApplicant = spouseUser.getName() + "님이 부부 IRP 연동을 수락했습니다.";
            notificationService.createNotification(applicantUserId, messageToApplicant, null, linkId);
            sseService.send(applicantUserId, "link_completed", messageToApplicant);

        } else { // REJECT
            link.setLinkStatus(LinkStatus.REJECTED_SPOUSE); // 상태를 '배우자 거절'로 변경

            // 신청자에게 보낼 '연동 거절' 알림
            messageToApplicant = spouseUser.getName() + "님이 부부 IRP 연동을 거절했습니다.";
            notificationService.createNotification(applicantUserId, messageToApplicant, null, linkId);
            sseService.send(applicantUserId, "link_rejected", messageToApplicant);
        }
        
        linkRepo.save(link);

        // 배우자 본인이 받았던 최초의 연동 요청 알림을 '읽음'으로 처리
        notificationRepo.findFirstByLinkIdAndIsReadIsFalseOrderByIdDesc(linkId)
            .ifPresent(notification -> {
                notification.setRead(true);
            });

        ObjectNode out = om.createObjectNode();
        out.put("linkId", link.getId());
        out.put("status", link.getLinkStatus().name());
        return out;
    }
    
    /**
     * 사용자가 OCR 3회 실패 후 관리자 검토를 직접 요청
     * @param linkId 검토를 요청할 연동 신청 ID
     * @param applicantInputSpouseName 신청자가 마지막으로 입력한 배우자 이름
     * @param applicantInputSpouseBirth 신청자가 마지막으로 입력한 배우자 생년월일
     */
    @Transactional
    public void requestAdminReview(Long linkId,
                                   String applicantInputSpouseName,
                                   String applicantInputSpouseBirth,
                                   String filename,
                                   String contentType,
                                   String base64) {
        IrpSpouseLink link = linkRepo.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));

        if (link.getOcrAttemptCount() < 3) {
            throw new IllegalStateException("3회 이상 실패한 경우에만 요청할 수 있습니다.");
        }
        if (link.getLinkStatus() == LinkStatus.PENDING_ADMIN) {
            throw new IllegalStateException("이미 검토 요청이 완료되었습니다.");
        }

        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("관리자 심사 요청에는 가족관계증명서 파일이 필요합니다.");
        }

        // 배우자 매핑(관리자 검토 전이라도 확정 가능한 경우 미리 매핑)
        LocalDate spouseBirth = LocalDate.parse(applicantInputSpouseBirth, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        UserEntity spouseUser = userRepo.findByNameAndBirthDate(applicantInputSpouseName, spouseBirth)
                .orElseThrow(() -> new IllegalStateException("입력하신 정보와 일치하는 배우자 회원이 없습니다. 정보를 확인 후 다시 요청해주세요."));

        link.setSpouseUserId(spouseUser.getUserId());

        // ✅ 이 시점에서만 S3 업로드
        byte[] bytes = Base64.getDecoder().decode(base64);
        String key = s3Service.put("family-doc/", filename, bytes);
        link.setS3ObjectKey(key);

        link.setLinkStatus(LinkStatus.PENDING_ADMIN);

        LinkAdminReview rv = new LinkAdminReview();
        rv.setLinkId(link.getId());
        rv.setReviewStatus(ReviewStatus.PENDING);
        rv.setReason(String.format("사용자 입력 정보: 이름=%s, 생년월일=%s", applicantInputSpouseName, applicantInputSpouseBirth));
        rv.setRequestedAt(LocalDate.now());

        reviewRepo.save(rv);
        linkRepo.save(link);
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