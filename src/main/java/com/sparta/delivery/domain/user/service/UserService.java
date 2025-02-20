package com.sparta.delivery.domain.user.service;

import com.querydsl.core.BooleanBuilder;
import com.sparta.delivery.config.auth.PrincipalDetails;
import com.sparta.delivery.config.global.exception.custom.ForbiddenException;
import com.sparta.delivery.config.global.exception.custom.UserNotFoundException;
import com.sparta.delivery.config.jwt.JwtUtil;
import com.sparta.delivery.domain.token.entity.RefreshToken;
import com.sparta.delivery.domain.token.repository.RefreshTokenRepository;
import com.sparta.delivery.domain.user.dto.*;
import com.sparta.delivery.domain.user.entity.QUser;
import com.sparta.delivery.domain.user.entity.User;
import com.sparta.delivery.domain.user.enums.UserRoles;
import com.sparta.delivery.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder  passwordEncoder;
    private final JwtUtil jwtUtil;

    private final Long accessExpiredMs;
    private final Long refreshExpiredMs;

    public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       @Value("${spring.jwt.accessTokenValidityInMilliseconds}") Long accessExpiredMs,
                       @Value("${spring.jwt.refreshTokenValidityInMilliseconds}") Long refreshExpiredMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.accessExpiredMs = accessExpiredMs;
        this.refreshExpiredMs = refreshExpiredMs;
    }

    public UserResDto signup(SignupReqDto signupReqDto) {

        if (userRepository.existsByUsername(signupReqDto.getUsername())){
            throw new IllegalArgumentException("username already exists : " + signupReqDto.getUsername());
        }

        User user = User.builder()
                .email(signupReqDto.getEmail())
                .password(passwordEncoder.encode(signupReqDto.getPassword()))
                .username(signupReqDto.getUsername())
                .nickname(signupReqDto.getNickname())
                .role(UserRoles.ROLE_CUSTOMER)
                .build();
        // 권한은 관리자 검증 후 관리자가 권한을 변경하는 방법으로 하겠습니다.
        // 최초의 MASTER 는 DB에서 직접 설정

        return userRepository.save(user).toResponseDto();
    }

    public JwtResponseDto authenticateUser(LoginRequestDto loginRequestDto) {

        User user = userRepository.findByUsernameAndDeletedAtIsNull(loginRequestDto.getUsername())
                .orElseThrow(()-> new IllegalArgumentException("Invalid username : " + loginRequestDto.getUsername()));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(),user.getPassword() )){
            throw new IllegalArgumentException("Invalid password : " + loginRequestDto.getPassword());
        }

        String accessToken = jwtUtil.createJwt("access",user.getUsername(), user.getEmail(), user.getRole(),accessExpiredMs);
        String refreshToken = jwtUtil.createJwt("refresh",user.getUsername(), user.getEmail(), user.getRole(),refreshExpiredMs);

        addRefreshTokenEntity(user,refreshToken);

        return new JwtResponseDto(accessToken,refreshToken);
    }

    @Transactional(readOnly = true)
    public UserResDto getUserById(UUID id) {

        User user = userRepository.findByUserIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new UserNotFoundException("User Not Found By Id : " + id));

        return user.toResponseDto();
    }

    @Transactional(readOnly = true)
    public Page<UserResDto> getUsers(UserSearchReqDto userSearchReqDto) {
        QUser qUser = QUser.user;

        BooleanBuilder builder = buildSearchConditions(userSearchReqDto,qUser);

        // 페이지네이션 설정
        Sort sort = getSortOrder(userSearchReqDto);

        PageRequest pageRequest = PageRequest.of(userSearchReqDto.getPage(), userSearchReqDto.getSize(),sort);

        // 유저 목록 조회 (페이징 + 검색 조건)
        Page<User> userPages = userRepository.findAll(builder,pageRequest);

        if (userPages.isEmpty()){
            throw new UserNotFoundException("Users Not Found");
        }

        return userPages.map(User :: toResponseDto);
    }

    public UserResDto updateUser(UUID id, PrincipalDetails principalDetails, UserUpdateReqDto userUpdateReqDto) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new UserNotFoundException("User Not Found By Id : " + id));

        if (!user.getUsername().equals(principalDetails.getUsername()) && !principalDetails.getRole().name().equals("ROLE_MASTER")){
            throw new ForbiddenException("Access denied.");
        }

        if (!passwordEncoder.matches(userUpdateReqDto.getCurrentPassword(),user.getPassword())){
            throw new ForbiddenException("Incorrect password.");
        }

        User updateUser = user.toBuilder()
                .password(passwordEncoder.encode(userUpdateReqDto.getNewPassword()))
                .email(userUpdateReqDto.getEmail())
                .nickname(userUpdateReqDto.getNickname())
                .build();

        return userRepository.save(updateUser).toResponseDto();
    }

    public UserResDto updateRole(UUID id, PrincipalDetails principalDetails, UserRoleUpdateReqDto userRoleUpdateReqDto) {
        if (!principalDetails.getRole().name().equals("ROLE_MASTER")){
            throw new ForbiddenException("Access denied.");
        }

        User user = userRepository.findByUserIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new UserNotFoundException("User Not Found By Id : " + id));

        User updateUser = user.toBuilder()
                .role(userRoleUpdateReqDto.getRole())
                .build();

        return userRepository.save(updateUser).toResponseDto();
    }

    public void deleteUser(UUID id, PrincipalDetails principalDetails) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new UserNotFoundException("User Not Found By Id : " + id));

        if (!user.getUsername().equals(principalDetails.getUsername()) &&
                !principalDetails.getRole().name().equals("ROLE_MASTER") &&
                !principalDetails.getRole().name().equals("ROLE_MANAGER")){
            throw new ForbiddenException("Access denied.");
        }

        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(principalDetails.getUsername());

        userRepository.save(user);
    }


    private void addRefreshTokenEntity(User user, String refresh){

        Date date = new Date(System.currentTimeMillis() + refreshExpiredMs);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .refresh(refresh)
                .expiration(date.toString())
                .build();

        refreshTokenRepository.save(refreshToken);
    }


    private BooleanBuilder buildSearchConditions(UserSearchReqDto userSearchReqDto, QUser qUser) {
        BooleanBuilder builder = new BooleanBuilder();

        // username 조건
        if (userSearchReqDto.getUsername() != null && !userSearchReqDto.getUsername().isEmpty()){
            builder.and(qUser.username.containsIgnoreCase(userSearchReqDto.getUsername()));
        }

        // email 조건
        if (userSearchReqDto.getEmail() != null && !userSearchReqDto.getEmail().isEmpty()){
            builder.and(qUser.username.containsIgnoreCase(userSearchReqDto.getEmail()));
        }

        // role 조건
        if (userSearchReqDto.getRole() != null){
            builder.and(qUser.role.eq(userSearchReqDto.getRole()));
        }

        builder.and(qUser.deletedAt.isNull());

        return builder;
    }

    private Sort getSortOrder(UserSearchReqDto userSearchReqDto) {
        String sortBy = userSearchReqDto.getSortBy();

        if (!isValidSortBy(sortBy)) {
            throw new IllegalArgumentException("SortBy 는 'createdAt', 'updatedAt', 'deletedAt' 값만 허용합니다.");
        }

        Sort sort = Sort.by(Sort.Order.by(sortBy));

        sort = getSortDirection(sort, userSearchReqDto.getOrder());

        return sort;
    }

    private boolean isValidSortBy(String sortBy) {
        return "createdAt".equals(sortBy) || "updatedAt".equals(sortBy) || "deletedAt".equals(sortBy);
    }

    private Sort getSortDirection(Sort sort, String order) {
        if (order.equals("desc")){
            return sort.descending();
        }else{
            return sort.ascending();
        }
    }
}
