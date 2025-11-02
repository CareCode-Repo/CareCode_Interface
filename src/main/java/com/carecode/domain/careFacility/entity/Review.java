package com.carecode.domain.careFacility.entity;

import com.carecode.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 시설 리뷰 엔티티
 * 
 * 돌봄 시설에 대한 사용자 리뷰를 관리.
 * @author CareCode Team
 * @since 1.0.0
 * @see CareFacility
 * @see User
 */
@Entity
@Table(name = "TBL_REVIEWS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Review {

    /**
     * 리뷰 고유 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 리뷰가 작성된 돌봄 시설
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FACILITY_ID", nullable = false)
    private CareFacility careFacility;

    /**
     * 리뷰를 작성한 사용자
     * 
     * <p>리뷰와 사용자 간의 다대일 관계를 나타냅니다.
     * LAZY 로딩을 사용하여 성능을 최적화합니다.</p>
     * 
     * <p>사용자가 삭제되면 관련된 모든 리뷰도 함께 삭제됩니다.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    /**
     * 리뷰 평점
     * 
     * <p>사용자가 시설에 대해 매긴 평점으로, 1점부터 5점까지 가능합니다.
     * 1점: 매우 불만족, 5점: 매우 만족</p>
     * 
     * <p>시설의 전체 평점 계산에 사용되며, 리뷰 검색 및 정렬에도 활용됩니다.</p>
     */
    @Column(name = "RATING", nullable = false)
    private Integer rating;

    /**
     * 리뷰 내용
     * 
     * <p>사용자가 작성한 리뷰의 상세 내용입니다.
     * TEXT 타입으로 설정하여 긴 리뷰도 저장할 수 있습니다.</p>
     * 
     * <p>null 값이 허용되며, 평점만 있는 리뷰도 가능합니다.</p>
     */
    @Column(name = "CONTENT", columnDefinition = "TEXT")
    private String content;

    /**
     * 리뷰 검증 여부
     * 
     * <p>리뷰가 관리자에 의해 검증되었는지 여부를 나타냅니다.
     * 검증된 리뷰만 UI에 표시되며, 신뢰도가 높습니다.</p>
     * 
     * <p>기본값은 false이며, 관리자가 수동으로 검증합니다.</p>
     */
    @Column(name = "IS_VERIFIED", nullable = false)
    private Boolean isVerified = false;

    /**
     * 리뷰 활성 상태 여부
     * 
     * <p>리뷰의 활성/비활성 상태를 나타냅니다.
     * false인 경우 UI에서 숨겨지고 검색에서 제외됩니다.</p>
     * 
     * <p>부적절한 리뷰나 신고된 리뷰를 비활성화할 때 사용됩니다.</p>
     */
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive = true;

    /**
     * 리뷰 작성 일시
     * 
     * <p>리뷰가 데이터베이스에 처음 저장된 시간입니다.
     * JPA Auditing에 의해 자동으로 설정되며, 수정할 수 없습니다.</p>
     */
    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 리뷰 수정 일시
     */
    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    /**
     * 리뷰 생성자
     * 
     * @param careFacility 리뷰가 작성된 돌봄 시설 (필수)
     * @param user 리뷰를 작성한 사용자 (필수)
     * @param rating 리뷰 평점 (1-5점, 필수)
     * @param content 리뷰 내용 (선택사항)
     * 
     * @throws IllegalArgumentException rating이 1-5 범위를 벗어나는 경우
     * @throws IllegalArgumentException careFacility 또는 user가 null인 경우
     */
    @Builder
    public Review(CareFacility careFacility, User user, Integer rating, String content) {
        this.careFacility = careFacility;
        this.user = user;
        this.rating = rating;
        this.content = content;
    }

    /**
     * 리뷰 정보 업데이트
     * 
     * @param rating 새로운 평점 (1-5점, null이면 기존 값 유지)
     * @param content 새로운 리뷰 내용 (null이면 기존 값 유지)
     * 
     * @throws IllegalArgumentException rating이 1-5 범위를 벗어나는 경우
     */
    public void updateReview(Integer rating, String content) {
        this.rating = rating;
        this.content = content;
    }

    /**
     * 리뷰 검증
     */
    public void verify() {
        this.isVerified = true;
    }

    /**
     * 리뷰 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 리뷰 활성화
     */
    public void activate() {
        this.isActive = true;
    }
} 