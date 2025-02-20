package com.sparta.delivery.domain.token.service;

import com.sparta.delivery.config.global.exception.custom.InvalidRefreshTokenException;
import com.sparta.delivery.config.global.exception.custom.RefreshTokenAlreadyExistsException;
import com.sparta.delivery.domain.token.entity.RefreshToken;
import com.sparta.delivery.domain.token.interfaces.RefreshTokenService;
import com.sparta.delivery.domain.token.repository.RefreshTokenRepository;
import com.sparta.delivery.domain.user.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtUtil jwtUtil;
    private final Long accessExpiredMs;
    private final Long refreshExpiredMs;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
                                   JwtUtil jwtUtil,
                                   @Value("${spring.jwt.accessTokenValidityInMilliseconds}") Long accessExpiredMs,
                                   @Value("${spring.jwt.refreshTokenValidityInMilliseconds}") Long refreshExpiredMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.accessExpiredMs = accessExpiredMs;
        this.refreshExpiredMs = refreshExpiredMs;
    }


    @Override
    public void addRefreshTokenEntity(User user, String refresh) {

        // 사용자의 RefreshToken 이 이미 존재하는지 확인 (로그인이 되어있는 경우)
        if (refreshTokenRepository.findByUser(user).isPresent()){

            // 기존 RefreshToken이 존재하는 경우, 해당 토큰이 만료되었는지 확인
            if (!jwtUtil.isExpired(refresh)){
                // 토큰이 아직 만료되지 않았다면 이미 로그인된 상태
                throw new RefreshTokenAlreadyExistsException("이미 로그인되었거나 비정상 로그아웃되었습니다.");
            }
        }

        // 기존 RefreshToken이 있지만, 만료된 경우 새로운 RefreshToken 발급
        Date date = new Date(System.currentTimeMillis() + refreshExpiredMs);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .refresh(refresh)
                .expiration(date.toString())
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    @Override
    public void removeRefreshToken(String refreshToken) {
        if(!refreshTokenRepository.existsByRefresh(refreshToken)){
            throw new InvalidRefreshTokenException("등록된 토큰이 아닙니다.");
        }

        refreshTokenRepository.deleteByRefresh(refreshToken);

        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
    }

    @Override
    public String reissueAccessToken(String refreshToken) {

        if (jwtUtil.isExpired(refreshToken)){
            throw new ExpiredJwtException(null, null, "Refresh token is still valid, no need to reissue access token");
        }

        if (!jwtUtil.getCategory(refreshToken).equals("refresh")){
            throw new InvalidRefreshTokenException("Provided token is not a refresh token");
        }

        RefreshToken token = refreshTokenRepository.findByRefresh(refreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid or non-existent refresh token"));

        User user = token.getUser();

        return jwtUtil.createJwt("access",user.getUsername(),user.getEmail(),user.getRole(),accessExpiredMs);
    }
}
