package com.carecode.domain.user.app;

import com.carecode.domain.user.dto.response.UserDto;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;

    @Transactional(readOnly = true)
    public UserDto getUserById(String userId) {
        return userService.getUserById(userId);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        return userService.getUserByEmail(email);
    }

    @Transactional
    public void updateProfileImage(String userId, String profileImageUrl) {
        userService.updateProfileImage(userId, profileImageUrl);
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

    @Transactional
    public void reactivateUser(String userId) {
        userService.reactivateUser(userId);
    }

    @Transactional(readOnly = true)
    public User getUserEntityByEmail(String email) {
        return userService.getUserEntityByEmail(email);
    }
}


