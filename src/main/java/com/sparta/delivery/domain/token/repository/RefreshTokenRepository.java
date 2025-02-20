package com.sparta.delivery.domain.token.repository;

import com.sparta.delivery.domain.token.entity.RefreshToken;
import com.sparta.delivery.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken , UUID> {

    Optional<RefreshToken> findByUser(User user);

    Optional<RefreshToken> findByRefresh(String refresh);

    Boolean existsByRefresh(String refresh);

    void deleteByRefresh(String refresh);

}
