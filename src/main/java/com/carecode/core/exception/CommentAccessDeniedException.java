package com.carecode.core.exception;

/**
 * 댓글에 대한 접근(수정/삭제) 권한이 없을 때 발생하는 예외
 */
public class CommentAccessDeniedException extends CareCodeException {

    public CommentAccessDeniedException() {
        super(ErrorCode.COMMENT_ACCESS_DENIED);
    }

    public CommentAccessDeniedException(String message) {
        super(ErrorCode.COMMENT_ACCESS_DENIED, message);
    }
}
