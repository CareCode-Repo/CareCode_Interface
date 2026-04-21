package com.carecode.domain.user.service;

import com.carecode.domain.user.dto.response.TokenDto;
import com.carecode.domain.user.entity.User;

public interface AuthService {

    TokenDto oAuthLoginOrRegister(String accessCode);

    TokenDto issueTokenForUser(User user, String message);
}
