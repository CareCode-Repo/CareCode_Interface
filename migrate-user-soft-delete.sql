-- 사용자 테이블에 소프트 삭제를 위한 deletedAt 컬럼 추가
ALTER TABLE tbl_user ADD COLUMN deleted_at DATETIME NULL COMMENT '소프트 삭제를 위한 삭제 시간';

-- 기존 삭제된 사용자가 있다면 deleted_at을 설정 (예시)
-- UPDATE tbl_user SET deleted_at = NOW() WHERE is_active = false AND deleted_at IS NULL;

-- 인덱스 추가 (성능 최적화)
CREATE INDEX idx_user_deleted_at ON tbl_user(deleted_at);
CREATE INDEX idx_user_active_not_deleted ON tbl_user(is_active, deleted_at);

-- 기존 사용자들의 deleted_at을 NULL로 설정 (삭제되지 않은 상태)
UPDATE tbl_user SET deleted_at = NULL WHERE deleted_at IS NULL;
