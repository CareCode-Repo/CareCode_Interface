package com.carecode.domain.community.service;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.community.dto.request.ReportCreateRequest;
import com.carecode.domain.community.dto.response.ReportResponse;
import com.carecode.domain.community.entity.Comment;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.PostStatus;
import com.carecode.domain.community.entity.Report;
import com.carecode.domain.community.entity.UserBlock;
import com.carecode.domain.community.repository.CommentRepository;
import com.carecode.domain.community.repository.PostRepository;
import com.carecode.domain.community.repository.ReportRepository;
import com.carecode.domain.community.repository.UserBlockRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 커뮤니티 모더레이션: 신고 접수·처리, 사용자 차단. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModerationService {

    private final ReportRepository reportRepository;
    private final UserBlockRepository userBlockRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CurrentUserFacade currentUserFacade;

    /** 이 횟수 이상 신고되면 관리자 확인 전까지 자동으로 숨긴다. */
    @Value("${app.community.auto-hide-report-threshold:5}")
    private long autoHideThreshold;

    // ====================
    // 신고 ====================
    @Transactional
    public ReportResponse report(ReportCreateRequest request) {
        User reporter = currentUserFacade.requireCurrentUser();

        User targetAuthor = resolveTargetAuthor(request.getTargetType(), request.getTargetId());
        if (targetAuthor != null && targetAuthor.getId().equals(reporter.getId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "본인의 글은 신고할 수 없습니다.");
        }

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporter.getId(), request.getTargetType(), request.getTargetId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 신고한 대상입니다.");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .detail(request.getDetail())
                .status(Report.ReportStatus.PENDING)
                .build();

        Report saved = reportRepository.save(report);

        applyAutoHideIfNeeded(request.getTargetType(), request.getTargetId());

        return ReportResponse.from(saved);
    }

    public Page<ReportResponse> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtAsc(Report.ReportStatus.PENDING, pageable)
                .map(ReportResponse::from);
    }

    /** 관리자 처리: 수용(대상 숨김) 또는 반려(숨김 해제). */
    @Transactional
    public ReportResponse resolve(Long reportId, Report.ReportStatus status, String note) {
        if (status != Report.ReportStatus.ACCEPTED && status != Report.ReportStatus.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "처리 상태는 ACCEPTED 또는 REJECTED 여야 합니다.");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("신고를 찾을 수 없습니다: " + reportId));

        report.resolve(status, note);

        if (status == Report.ReportStatus.ACCEPTED) {
            hideTarget(report.getTargetType(), report.getTargetId());
        }
        return ReportResponse.from(reportRepository.save(report));
    }

    // ====================
    // 차단 ====================
    @Transactional
    public void blockUser(Long targetUserId) {
        User blocker = currentUserFacade.requireCurrentUser();
        if (blocker.getId().equals(targetUserId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "자기 자신은 차단할 수 없습니다.");
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), targetUserId)) {
            return;
        }

        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + targetUserId));

        userBlockRepository.save(UserBlock.builder().blocker(blocker).blocked(blocked).build());
    }

    @Transactional
    public void unblockUser(Long targetUserId) {
        User blocker = currentUserFacade.requireCurrentUser();
        userBlockRepository.findByBlockerIdAndBlockedId(blocker.getId(), targetUserId)
                .ifPresent(userBlockRepository::delete);
    }

    /** 현재 사용자가 차단한 사용자 ID 목록. 목록 API 필터링에 사용한다. */
    public List<Long> getBlockedUserIds() {
        User blocker = currentUserFacade.requireCurrentUser();
        return userBlockRepository.findBlockedUserIds(blocker.getId());
    }

    // ====================
    // 내부 ====================
    private void applyAutoHideIfNeeded(Report.TargetType targetType, Long targetId) {
        long reportCount = reportRepository.countActiveReports(targetType, targetId);
        if (reportCount >= autoHideThreshold) {
            log.warn("신고 누적으로 자동 숨김 처리 - type={}, id={}, 신고수={}", targetType, targetId, reportCount);
            hideTarget(targetType, targetId);
        }
    }

    private void hideTarget(Report.TargetType targetType, Long targetId) {
        if (targetType == Report.TargetType.POST) {
            postRepository.findById(targetId).ifPresent(post -> {
                post.setIsActive(false);
                post.setStatus(PostStatus.HIDDEN);
                postRepository.save(post);
            });
        } else {
            commentRepository.findById(targetId).ifPresent(comment -> {
                comment.updateStatus(Comment.CommentStatus.HIDDEN);
                commentRepository.save(comment);
            });
        }
    }

    private User resolveTargetAuthor(Report.TargetType targetType, Long targetId) {
        if (targetType == Report.TargetType.POST) {
            Post post = postRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다: " + targetId));
            return post.getAuthor();
        }
        Comment comment = commentRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다: " + targetId));
        return comment.getAuthor();
    }
}
