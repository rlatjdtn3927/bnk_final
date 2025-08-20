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

            // 1. OCR 결과에서 검증된 배우자 정보로 시스템 내에서 사용자 조회
            String spouseNameOcr = rs.getSpouseName();
            // OCR 결과의 날짜 형식이 'yyyy-MM-dd'라고 가정. 실제 형식에 맞게 변경 필요.
            LocalDate spouseBirthOcr = LocalDate.parse(rs.getSpouseBirth(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            UserEntity spouseUser = userRepo.findByNameAndBirthDate(spouseNameOcr, spouseBirthOcr)
                    .orElseThrow(() -> new IllegalStateException("연동하려는 배우자가 서비스에 가입되어 있지 않습니다."));

            Long spouseUserId = spouseUser.getUserId();
            
            // 2. 조회된 배우자 ID를 연동 테이블에 저장
            link.setSpouseUserId(spouseUserId);
            link.setLinkStatus(LinkStatus.PENDING_SPOUSE);
            linkRepo.save(link);

            // 3. 알림 생성 (DB 저장)
            // 신청자 정보를 다시 조회하여 알림 메시지에 사용
            UserEntity applicantUser = userRepo.findById(applicantUserId)
                                            .orElseThrow(() -> new IllegalStateException("신청자 정보를 찾을 수 없습니다."));
            String message = applicantUser.getName() + "님이 부부 IRP 연동을 신청했습니다.";
            String linkUrl = "/link-view/pending"; // 배우자가 알림 클릭 시 이동할 페이지 URL
            
            Notification noti = new Notification();
            noti.setUserId(spouseUserId);
            noti.setMessage(message);
            noti.setLinkUrl(linkUrl);
            noti.setLinkId(link.getId());
            
            notificationRepo.save(noti);

            // 4. 실시간 SSE 알림 발송 (사용자가 온라인일 경우를 위함)
            sseService.send(spouseUserId, "new_link_request", message);


            // 5. 최종 응답
            out.put("status", LinkStatus.PENDING_SPOUSE.name());
            out.put("nextAction", "WAIT_SPOUSE");
            out.put("message", "배우자 수락 대기");

        } else {
            // 관리자 심사 대기 로직은 그대로 유지됩니다.
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
        // 이제 action은 'REJECT'만 가능합니다.
        if (!"REJECT".equalsIgnoreCase(action)) {
            throw new IllegalArgumentException("관리자는 반려 처리만 할 수 있습니다.");
        }

        IrpSpouseLink link = linkRepo.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("연동 신청이 존재하지 않습니다."));

        LinkAdminReview rv = reviewRepo.findTopByLinkIdOrderByIdDesc(linkId)
                .orElseThrow(() -> new IllegalStateException("검토 요청 내역이 없습니다."));

        if (rv.getReviewStatus() != ReviewStatus.PENDING) {
            throw new IllegalStateException("이미 심사 완료된 건입니다.");
        }
        
        // --- 반려 처리 로직 ---
        link.setLinkStatus(LinkStatus.REJECTED_ADMIN);
        
        rv.setReviewStatus(ReviewStatus.REJECTED);
        String rejectionReason = (reason != null && !reason.isBlank()) ? reason : "관리자 반려";
        rv.setReason(rejectionReason);
        rv.setAdminMemo(reason);
        rv.setCompletedAt(LocalDate.now());

        // 신청자에게 반려 알림 생성 및 발송
        Long applicantUserId = link.getApplicantUserId();
        String message = "관리자가 부부 IRP 연동 신청을 반려했습니다. 사유: " + rejectionReason;
        
        notificationService.createNotification(applicantUserId, message, "/link-view/apply");
        sseService.send(applicantUserId, "admin_rejected", message);

        linkRepo.save(link);
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
        
        if ("ACCEPT".equalsIgnoreCase(action)) {
            link.setLinkStatus(LinkStatus.LINKED); // 상태를 '연동 완료'로 변경
            link.setApprovedAt(LocalDate.now());   // 연동 완료일 기록
            
            // 1. 신청자의 ID를 가져옴
            Long applicantUserId = link.getApplicantUserId();

            // 2. 알림 메시지에 사용할 배우자(본인)의 이름을 조회
            UserEntity spouseUser = userRepo.findById(spouseUserId)
                                          .orElseThrow(() -> new IllegalStateException("배우자 정보를 찾을 수 없습니다."));
            String message = spouseUser.getName() + "님이 부부 IRP 연동을 수락했습니다.";
            
            // 3. 신청자에게 갈 알림을 DB에 저장
            Notification notiToApplicant = new Notification();
            notiToApplicant.setUserId(applicantUserId);
            notiToApplicant.setMessage(message);
            notiToApplicant.setLinkId(linkId);
            // notiToApplicant.setLinkUrl("/link-view/status"); 
            notificationRepo.save(notiToApplicant);

            // 4. 신청자에게 실시간 SSE 알림을 발송합니다.
            sseService.send(applicantUserId, "link_completed", message);
        } else {
            link.setLinkStatus(LinkStatus.REJECTED_SPOUSE); // 상태를 '배우자 거절'로 변경
        }
        linkRepo.save(link);

        // 이 연동(linkId)과 관련된 '읽지 않은' 알림을 찾아 '읽음'으로 처리
        notificationRepo.findFirstByLinkIdAndIsReadIsFalseOrderByIdDesc(linkId)
            .ifPresent(notification -> {
                notification.setRead(true);
            });

        ObjectNode out = om.createObjectNode();
        out.put("linkId", link.getId());
        out.put("status", link.getLinkStatus().name());
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