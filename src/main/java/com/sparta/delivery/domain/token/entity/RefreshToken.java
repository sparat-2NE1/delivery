package com.sparta.delivery.domain.token.entity;

import com.sparta.delivery.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "p_refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID RefreshTokenId;

    // 토큰의 주인 유저
    // 각 유저 당 하나의 리프레쉬 토큰만 가짐
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // refresh 토큰 키 값
    @Column(nullable = false , length = 512)
    private String refresh;

    // refresh 토큰 유효시간
    @Column(nullable = false)
    private String expiration;
}
