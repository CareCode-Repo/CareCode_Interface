package com.carecode.domain.user.service;

import com.carecode.domain.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    User oAuthLoginOrRegister(String accessCode, HttpServletResponse httpServletResponse);
}
