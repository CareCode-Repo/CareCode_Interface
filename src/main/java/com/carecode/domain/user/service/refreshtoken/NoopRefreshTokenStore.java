package com.carecode.domain.user.service.refreshtoken;

public class NoopRefreshTokenStore implements RefreshTokenStore {

    @Override
    public void register(String userId, String refreshTokenJwt) {
        // no-op
    }

    @Override
    public boolean isRegistered(String refreshTokenJwt, String userId) {
        return true;
    }

    @Override
    public void remove(String refreshTokenJwt, String userId) {
        // no-op
    }

    @Override
    public void removeAllForUser(String userId) {
        // no-op
    }
}
