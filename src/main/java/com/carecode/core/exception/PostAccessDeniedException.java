package com.carecode.core.exception;

/**
 * 게시글에 대한 접근(수정/삭제) 권한이 없을 때 발생하는 예외
 */
public class PostAccessDeniedException extends CareCodeException {

    public PostAccessDeniedException() {
        super(ErrorCode.POST_ACCESS_DENIED);
    }

    public PostAccessDeniedException(String message) {
        super(ErrorCode.POST_ACCESS_DENIED, message);
    }
}
