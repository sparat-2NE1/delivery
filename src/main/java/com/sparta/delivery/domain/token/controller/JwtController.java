package com.sparta.delivery.domain.token.controller;

import com.sparta.delivery.domain.token.service.RefreshTokenServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class JwtController {

    private final RefreshTokenServiceImpl refreshTokenService;

    @PostMapping("/reissue")
    public ResponseEntity<?> reissueAccessToken(@CookieValue(name = "refresh", defaultValue = "")String refreshToken){

        String newAccessToken = refreshTokenService.reissueAccessToken(refreshToken);

        return ResponseEntity.status(HttpStatus.OK)
                .header("Authorization", newAccessToken)
                .build();
    }

}
