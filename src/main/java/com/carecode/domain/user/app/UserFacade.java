package com.carecode.domain.user.app;

import com.carecode.core.storage.FileStorageService;
import com.carecode.core.storage.StoredFile;
import com.carecode.domain.user.dto.response.ProfileImageResponse;
import com.carecode.domain.user.dto.response.UserDto;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserFacade {

    private static final String PROFILE_IMAGE_DIRECTORY = "profile-images";

    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        return userService.getUserByEmail(email);
    }

    @Transactional
    public void updateProfileImage(String userId, String profileImageUrl) {
        userService.updateProfileImage(userId, profileImageUrl);
    }

    /**
     * 프로필 이미지 파일을 저장하고 사용자에 연결한다.
     *
     * 기존 경로는 URL 문자열만 받아서, 클라이언트가 파일을 올릴 곳이 없었다.
     * 저장은 건강기록 첨부와 같은 FileStorageService 를 쓰므로 S3 로 옮길 때 함께 옮겨간다.
     */
    @Transactional
    public ProfileImageResponse uploadProfileImage(String userId, MultipartFile file) {
        StoredFile stored = fileStorageService.store(file, PROFILE_IMAGE_DIRECTORY);
        userService.updateProfileImage(userId, stored.getUrl());

        return ProfileImageResponse.builder()
                .profileImageUrl(stored.getUrl())
                .build();
    }

    @Transactional
    public UserDto updateUserLocation(String userId, Double latitude, Double longitude) {
        return userService.updateUserLocation(userId, latitude, longitude);
    }

    @Transactional
    public void deactivateUser(String userId) {
        userService.deactivateUser(userId);
    }

    @Transactional
    public void deleteUser(String userId) {
        userService.deleteUser(userId);
    }

    // 계정 복구는 관리자 전용이라 이 파사드를 거치지 않는다. AdminUserController 참고.

    @Transactional(readOnly = true)
    public User getUserEntityByEmail(String email) {
        return userService.getUserEntityByEmail(email);
    }
}

