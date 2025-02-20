package com.sparta.delivery.domain.token.interfaces;

import com.sparta.delivery.domain.user.entity.User;

public interface JwtService {

    String createAccessToken(User user);

    String createRefreshToken(User user);

    boolean isTokenExpired(String token);

    String getCategory(String token);
}
