package com.sparta.delivery.domain.token.service;

import com.sparta.delivery.domain.token.interfaces.JwtService;
import com.sparta.delivery.domain.user.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {

    private final JwtUtil jwtUtil;

    private final Long accessExpiredMs;
    private final Long refreshExpiredMs;

    public JwtServiceImpl(JwtUtil jwtUtil,
                          @Value("${spring.jwt.accessTokenValidityInMilliseconds}") Long accessExpiredMs,
                          @Value("${spring.jwt.refreshTokenValidityInMilliseconds}") Long refreshExpiredMs) {
        this.jwtUtil = jwtUtil;
        this.accessExpiredMs = accessExpiredMs;
        this.refreshExpiredMs = refreshExpiredMs;
    }

    @Override
    public String createAccessToken(User user) {
        return jwtUtil.createJwt("access",user.getUsername(), user.getEmail(), user.getRole(),accessExpiredMs);
    }

    @Override
    public String createRefreshToken(User user) {
        return jwtUtil.createJwt("refresh",user.getUsername(), user.getEmail(), user.getRole(),refreshExpiredMs);
    }

    @Override
    public boolean isTokenExpired(String token) {

        try{
            jwtUtil.isExpired(token);
            return false;
        }catch (ExpiredJwtException e){
            return true;
        }
    }

    @Override
    public String getCategory(String token) {
        return jwtUtil.getCategory(token);
    }
}
