package com.sparta.delivery.domain.token.interfaces;

import com.sparta.delivery.domain.user.entity.User;

public interface RefreshTokenService {

    void addRefreshTokenEntity(User user , String refreshToken);

    void removeRefreshToken(String refreshToken);

    String reissueAccessToken(String refreshToken);
}
