package com.example.memo.couple.service;

import static com.example.memo.couple.ocr.OcrTextUtil.normalizeBirth;
import static com.example.memo.couple.ocr.OcrTextUtil.normalizeNameForCompare;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.couple.ocr.dto.SpouseOcrRequest;
import com.example.memo.couple.ocr.dto.SpouseOcrResponse;
import com.example.memo.couple.ocr.service.ClovaSpouseOcrService;
import com.example.memo.jpa.entity.couple.IrpSpouseLink;
import com.example.memo.jpa.entity.couple.LinkAdminReview;
import com.example.memo.jpa.entity.couple.LinkStatus;
import com.example.memo.jpa.entity.couple.ReviewStatus;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.user.UserEntity;
import com.example.memo.jpa.repository.couple.IrpSpouseLinkRepository;
import com.example.memo.jpa.repository.couple.LinkAdminReviewRepository;
import com.example.memo.jpa.repository.couple.NotificationRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.jpa.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpouseLinkService {

    private final IrpSpouseLinkRepository linkRepo;
//    private final LinkAdminReviewRepository reviewRepo;
    private final UserRepository userRepo;
//    private final S3Service s3Service;
    private final ClovaSpouseOcrService spouseOcrService;
    private final ObjectMapper om;

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepo;
    private final SseService sseService;
    private final IrpAccountRepository irpAccountRepo;
    
    
    @Transactional(readOnly = true)
    public ObjectNode getStatus(Long userId) {
        ObjectNode out = om.createObjectNode();

        Optional<IrpAccount> irpOpt = irpAccountRepo.findByUser_UserId(userId);
        if (irpOpt.isPresent()) {
            IrpAccount account = irpOpt.get();
            out.put("hasIrpAccount", true);
            ObjectNode accountNode = out.putObject("account");
            accountNode.put("number", account.getIrpAcctNo()); // 엔티티 필드명에 맞게 getAccountNumber() -> getIrpAcctNo()
            accountNode.put("balance", account.getBalance().toPlainString());
        } else {
            out.put("hasIrpAccount", false);
        }


        // 새로 만든 Repository 메소드를 사용하여 최신 데이터를 조회
        Optional<IrpSpouseLink> opt = linkRepo.findLatestOneByUser(userId);

        if (opt.isEmpty()) {
            // 전혀 신청/연동 이력 없음
            out.put("linked", false);
            out.put("hasAny", false);
            return out;
        }

        IrpSpouseLink link = opt.get();
        String status = link.getLinkStatus().name();
        out.put("hasAny", true);
        out.put("linkId", link.getId());
        out.put("status", status);

        // 내가 신청자인지 배우자인지 역할(myRole)을 알려주는 로직
        if (Objects.equals(link.getApplicantUserId(), userId)) {
            out.put("myRole", "APPLICANT");
        } else if (Objects.equals(link.getSpouseUserId(), userId)) {
            out.put("myRole", "SPOUSE");
        }

        boolean isLinked = "LINKED".equals(status);
        out.put("linked", isLinked);

        Long spouseUserId;
        if (Objects.equals(link.getApplicantUserId(), userId)) {
            spouseUserId = link.getSpouseUserId();
        } else {
            spouseUserId = link.getApplicantUserId();
        }

        if (spouseUserId != null) {
            userRepo.findById(spouseUserId).ifPresent(spouse -> {
                out.put("spouseUserId", spouse.getUserId());
                out.put("spouseName", spouse.getName());
            });
        }

        if (link.getApprovedAt() != null) out.put("linkedAt", link.getApprovedAt().toString());
        if (link.getAppliedAt() != null)  out.put("appliedAt", link.getAppliedAt().toString());

        return out;
    }

    @Transactional
    public ObjectNode apply(
            Long linkId, Long applicantUserId,
            String spouseName, String spouseBirth,
            String filename, String contentType, String base64) {

        if (applicantUserId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        // 이미 연동완료인 경우 차단
        if (linkRepo.existsLinkedForUser(applicantUserId)) {
            throw new IllegalStateException("이미 연동 완료된 내역이 있습니다.");
        }

        // ✅ 신규/재시도 분기
        IrpSpouseLink link;
        if (linkId != null) {
            link = linkRepo.findById(linkId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));
            // 재시도 상한 체크
            if (link.getOcrAttemptCount() >= 3) {
                throw new IllegalStateException("최대 3회까지만 시도할 수 있습니다. 가까운 영업점을 방문해주세요.");
            }
        } else {
            // 신규 신청
            link = new IrpSpouseLink();
            link.setApplicantUserId(applicantUserId);
            link.setLinkStatus(LinkStatus.APPLIED);
            link.setAppliedAt(LocalDate.now());
            link.setOcrAttemptCount(0);
            link = linkRepo.save(link); // linkId 확보
        }

        // --- OCR 검증 ---
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
                    boolean selfNameOk    = safeEqualsName(me.getName(), rs.getApplicantName());
                    boolean selfBirthOk   = sameBirth(dateToYmd(me.getBirthDate()), rs.getApplicantBirth());
                    boolean spouseNameOk  = safeEqualsName(spouseName, rs.getSpouseName());
                    boolean spouseBirthOk = sameBirth(spouseBirth, rs.getSpouseBirth());
                    ocrOk = selfNameOk && selfBirthOk && spouseNameOk && spouseBirthOk;
                    if (!ocrOk) failReason = "OCR 정보 불일치";
                } else {
                    failReason = (rs != null && rs.getReason() != null) ? rs.getReason() : "OCR 실패 또는 정보 미검출";
                }
            } catch (Exception e) {
                failReason = "OCR 시스템 오류: " + e.getMessage();
            }
        } else {
            failReason = "증빙 서류 미첨부";
        }

        // 시도 횟수 증가
        link.setOcrAttemptCount(link.getOcrAttemptCount() + 1);

        ObjectNode out = om.createObjectNode().put("linkId", link.getId());

        if (ocrOk) {
            // ✅ OCR 성공 → 배우자 매핑 후 PENDING_SPOUSE
            String spouseNameOcr = rs.getSpouseName();
            LocalDate spouseBirthOcr = LocalDate.parse(rs.getSpouseBirth(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            UserEntity spouseUser = userRepo.findByNameAndBirthDate(spouseNameOcr, spouseBirthOcr)
                    .orElseThrow(() -> new IllegalStateException("연동하려는 배우자가 서비스에 가입되어 있지 않습니다."));

            link.setSpouseUserId(spouseUser.getUserId());
            link.setLinkStatus(LinkStatus.PENDING_SPOUSE);

            String message = me.getName() + "님이 부부 IRP 연동을 신청했습니다.";
            notificationService.createNotification(spouseUser.getUserId(), message, "/link-view/pending", link.getId());
            sseService.send(spouseUser.getUserId(), "new_link_request", message);

            out.put("status", LinkStatus.PENDING_SPOUSE.name());
            out.put("nextAction", "WAIT_SPOUSE");
            out.put("message", "배우자 수락 대기");
        } else {
            // ❌ OCR 실패
            if (link.getOcrAttemptCount() >= 3) {
                link.setLinkStatus(LinkStatus.REJECTED_OCR_FINAL);
                out.put("status", link.getLinkStatus().name());
                out.put("nextAction", "GUIDE_BRANCH_VISIT");
                out.put("message", "3회 인증에 실패했습니다. 가까운 영업점을 방문해주세요.");
            } else {
                // 계속 APPLIED 유지 (재시도)
                if (link.getLinkStatus() == null) link.setLinkStatus(LinkStatus.APPLIED);
                int remaining = 3 - link.getOcrAttemptCount();
                out.put("status", link.getLinkStatus().name());
                out.put("nextAction", "RETRY_OCR");
                out.put("message", String.format("OCR 판독 실패 (%d/3회). 선명한 사진으로 다시 시도해주세요.", link.getOcrAttemptCount()));
                out.put("remainingAttempts", remaining);
                if (failReason != null) out.put("failReason", failReason);
            }
        }

        linkRepo.save(link);
        return out;
    }

    @Transactional
    public ObjectNode initDraft(Long applicantUserId) {
        if (applicantUserId == null) throw new IllegalArgumentException("로그인이 필요합니다.");
        // 이미 LINKED면 차단
        if (linkRepo.existsLinkedForUser(applicantUserId)) {
            throw new IllegalStateException("이미 연동 완료된 내역이 있습니다.");
        }
        // 진행중 건이 있으면 그걸 반환 (APPLIED/PENDING_* 등)
        Optional<IrpSpouseLink> latest = linkRepo.findLatestOneByUser(applicantUserId);
        if (latest.isPresent() && latest.get().getLinkStatus() != LinkStatus.UNLINKED) {
            return om.createObjectNode().put("linkId", latest.get().getId());
        }

        IrpSpouseLink link = new IrpSpouseLink();
        link.setApplicantUserId(applicantUserId);
        link.setLinkStatus(LinkStatus.APPLIED);
        link.setAppliedAt(LocalDate.now());
        link.setOcrAttemptCount(0);
        link = linkRepo.save(link);

        return om.createObjectNode().put("linkId", link.getId());
    }

    
    /**
     * 관리자가 검토 대기중인 연동 신청 목록을 조회
     * (배우자 정보 조회 로직 추가)
     */
    /*
    public ArrayNode adminListPending(int urlTtlMinutes) {
        var links = linkRepo.findByLinkStatusOrderByAppliedAtDesc(LinkStatus.PENDING_ADMIN);
        if (links.isEmpty()) {
            return om.createArrayNode();
        }

        ArrayNode resultList = om.createArrayNode();
        for (IrpSpouseLink link : links) {
            ObjectNode item = om.createObjectNode();
            item.put("linkId", link.getId());
            item.put("requestedAt", link.getAppliedAt().toString());

            // 1. 신청자 정보 (DB 기준)
            ObjectNode applicantNode = item.putObject("applicant");
            userRepo.findById(link.getApplicantUserId()).ifPresent(user -> {
                applicantNode.put("id", user.getUserId());
                applicantNode.put("name", user.getName());
                applicantNode.put("birth", user.getBirthDate().toString());
            });

            // 2. 배우자 정보 (사용자 입력 + DB 조회 결과)
            ObjectNode spouseNode = item.putObject("spouse");
            reviewRepo.findTopByLinkIdOrderByIdDesc(link.getId()).ifPresent(review -> {
                String reason = review.getReason();
                spouseNode.put("userInputName", parseValue(reason, "이름"));
                spouseNode.put("userInputBirth", parseValue(reason, "생년월일"));
            });
            if (link.getSpouseUserId() != null) {
                userRepo.findById(link.getSpouseUserId()).ifPresent(user -> {
                    spouseNode.put("dbId", user.getUserId());
                    spouseNode.put("dbName", user.getName());
                    spouseNode.put("dbBirth", user.getBirthDate().toString());
                });
            }

            // 3. 증빙 서류 URL
            if (link.getS3ObjectKey() != null) {
                try {
                    String url = s3Service.presignedGetUrl(link.getS3ObjectKey(), java.time.Duration.ofMinutes(urlTtlMinutes));
                    item.put("proofUrl", url);
                } catch (Exception e) {
                    item.put("proofUrl", "#");
                }
            }
            resultList.add(item);
        }
        return resultList;
    }
    */

    /**
     * 관리자가 연동 신청을 승인하거나 반려
     * @param linkId 처리할 연동 신청 ID
     * @param spouseUserId 관리자가 확정한 배우자의 ID (승인 시에만 필요)
     * @param action "APPROVE" 또는 "REJECT"
     * @param reason 반려 사유 또는 관리자 메모
     * @return 처리 결과가 담긴 ObjectNode
     */
    /*
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
*/
    
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
    /*
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

        // 이 시점에서만 S3 업로드
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
    */
    
    /**
     * 특정 연동 요청의 상세 정보(주로 신청자 이름)를 조회
     * @param linkId 조회할 연동 요청 ID
     * @return 신청자 정보 등이 담긴 ObjectNode
     */
    @Transactional(readOnly = true)
    public ObjectNode getRequestDetails(Long linkId) {
        // 1. linkId로 연동 정보 조회
        IrpSpouseLink link = linkRepo.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 연동 신청입니다. (ID: " + linkId + ")"));

        // 2. 연동 정보에서 신청자 ID를 이용해 사용자 정보 조회
        UserEntity applicant = userRepo.findById(link.getApplicantUserId())
                .orElseThrow(() -> new IllegalStateException("신청자 정보를 찾을 수 없습니다."));

        // 3. 프론트엔드에 전달할 JSON 객체 생성
        ObjectNode details = om.createObjectNode();
        details.put("applicantUserId", applicant.getUserId());
        details.put("applicantName", applicant.getName());
        details.put("appliedAt", link.getAppliedAt().toString());

        return details;
    }
    
    /**
     * OCR 사전 검증만 수행하는 메소드
     * DB 저장 없이 OCR 서비스 호출 후 결과만 반환
     */
    @Transactional // DB 수정이 필요하므로 Transactional 추가
    public SpouseOcrResponse verifyOcrOnly(Long linkId, String base64, String filename, String contentType) {
        // 1. linkId로 신청 건 조회
        IrpSpouseLink link = linkRepo.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));

        // 2. 시도 횟수 확인 및 증가
        int currentCount = link.getOcrAttemptCount();
        if (currentCount >= 3) {
            throw new IllegalStateException("OCR 판독 시도 횟수(3회)를 초과했습니다.");
        }
        link.setOcrAttemptCount(currentCount + 1);
        linkRepo.save(link);

        // 3. OCR 서비스 호출
        SpouseOcrRequest rq = new SpouseOcrRequest();
        rq.setBase64(base64);
        rq.setFilename(filename);
        rq.setContentType(contentType);

        return spouseOcrService.parse(rq);
    }

    /**
     * 사용자가 연동된 부부 관계를 해지합니다.
     * @param linkId 해지할 연동 ID
     * @param requestingUserId 해지를 요청한 사용자의 ID
     * @return 처리 결과 (linkId, 최종 status)
     */
    @Transactional
    public ObjectNode unlink(Long linkId, Long requestingUserId) {
        // 1. 해지할 연동 정보 조회
        IrpSpouseLink link = linkRepo.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 연동입니다."));

        // 2. 보안 검증: 요청자가 실제 연동 당사자인지 확인
        boolean isParty = Objects.equals(requestingUserId, link.getApplicantUserId()) ||
                          Objects.equals(requestingUserId, link.getSpouseUserId());
        if (!isParty) {
            throw new SecurityException("연동 당사자 본인만 해지할 수 있습니다.");
        }

        // 3. 상태 검증: 'LINKED'(연동 완료) 상태일 때만 해지 가능
        if (link.getLinkStatus() != LinkStatus.LINKED) {
            throw new IllegalStateException("이미 해지되었거나 연동 완료 상태가 아니므로 해지할 수 없습니다.");
        }

        // 4. 해지 처리
        link.setLinkStatus(LinkStatus.UNLINKED);
        link.setUnlinkedAt(LocalDate.now()); // 해지일 기록
        linkRepo.save(link);

        // 5. 상대방 배우자에게 알림 발송
        UserEntity requester = userRepo.findById(requestingUserId)
                                       .orElseThrow(() -> new IllegalStateException("요청자 정보를 찾을 수 없습니다."));
        
        // 상대방 ID 결정
        Long otherPartyUserId = Objects.equals(requestingUserId, link.getApplicantUserId())
                ? link.getSpouseUserId()
                : link.getApplicantUserId();

        if (otherPartyUserId != null) {
            String message = requester.getName() + "님이 부부 IRP 연동을 해지했습니다.";
            notificationService.createNotification(otherPartyUserId, message, null, link.getId());
            sseService.send(otherPartyUserId, "link_unlinked", message);
        }

        // 6. 최종 결과 반환
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
    
    /**
     * "키1=값1, 키2=값2" 형태의 문자열에서 특정 키에 해당하는 값을 파싱하는 헬퍼 메소드
     * @param source "사용자 입력 정보: 이름=이배우, 생년월일=1990-11-20" 과 같은 문자열
     * @param key 찾고자 하는 키 (예: "이름")
     * @return 찾은 값 (예: "이배우") / 없으면 null
     */
    private String parseValue(String source, String key) {
        if (source == null || key == null || !source.contains(key + "=")) {
            return null;
        }
        try {
            // "key=" 뒷부분을 가져옴
            String part = source.split(key + "=")[1];
            // 콤마(,)가 있다면 그 앞부분까지만, 없다면 전체를 값으로 취급
            if (part.contains(",")) {
                return part.split(",")[0].trim();
            }
            return part.trim();
        } catch (Exception e) {
            // 파싱 중 오류 발생 시 null 반환
            log.error("Reason 필드 파싱 중 오류 발생: source={}, key={}", source, key, e);
            return null;
        }
    }
}