package com.sparta.delivery.domain.user.repository;

import com.sparta.delivery.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User , UUID> , QuerydslPredicateExecutor<User> {

    Optional<User> findByUsername(String username);

    // username 존재 유무 확인 (존재 : true , 없을 시 : false)
    boolean existsByUsername(String username);

    // 삭제되지 않은 유저 단일 조회
    Optional<User> findByUserIdAndDeletedAtIsNull(UUID id);

    // 삭제되지 않은 유저 단일 조회 (username)
    Optional<User> findByUsernameAndDeletedAtIsNull(String username);
}
