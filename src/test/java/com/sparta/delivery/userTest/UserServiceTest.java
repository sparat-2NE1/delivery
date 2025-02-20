package com.sparta.delivery.userTest;


import com.querydsl.core.BooleanBuilder;
import com.sparta.delivery.config.auth.PrincipalDetails;
import com.sparta.delivery.config.global.exception.custom.ForbiddenException;
import com.sparta.delivery.config.global.exception.custom.UserNotFoundException;
import com.sparta.delivery.domain.token.service.JwtUtil;
import com.sparta.delivery.domain.user.dto.*;
import com.sparta.delivery.domain.user.entity.User;
import com.sparta.delivery.domain.user.enums.UserRoles;
import com.sparta.delivery.domain.user.repository.UserRepository;
import com.sparta.delivery.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 테스트용 사용자 생성
        userId = UUID.randomUUID();
        testUser = User.builder()
                .userId(userId)
                .email("test@example.com")
                .password("encodedPassword")
                .username("testuser")
                .nickname("testnick")
                .role(UserRoles.ROLE_CUSTOMER)
                .build();
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void testSignupSuccess() {
        // Given
        SignupReqDto signupReqDto = new SignupReqDto("test@example.com", "testuser", "password", "testnick");

        when(userRepository.existsByUsername("testuser")).thenReturn(false); // 해당 username 존재 유무 확인
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword"); // 비밀번호 엄호화
        when(userRepository.save(any(User.class))).thenReturn(testUser); // user entity 에 저장

        // When
        UserResDto result = userService.signup(signupReqDto);

        // Then
        assertNotNull(result); // result 가 null 인지 검사
        assertEquals("test@example.com", result.getEmail()); // null 이 아닌경우 예상한 값과 email이 동일한지 검사
        assertEquals("testuser", result.getUsername()); // null 아닌 경우 예상한 값과 username이 동일한지 검사
    }

    @Test
    @DisplayName("회원가입 실패 테스트")
    void testSignupFail(){
        // Given
        SignupReqDto signupReqDto = new SignupReqDto("test@example.com", "testuser", "password", "testnick");


        // any() 권장
        when(userRepository.existsByUsername(any())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.signup(signupReqDto));
        assertEquals("username already exists : test@example.com", exception.getMessage());
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void testAuthenticateUserSuccess() {
        // Given
        LoginRequestDto loginRequestDto = new LoginRequestDto("newuser", "password");

        // 이미 존재하는 사용자 'newuser'를 mock 처리
        User newUser = User.builder()
                .userId(UUID.randomUUID())
                .email("newuser@example.com")
                .password("encodedPassword")  // 실제 비밀번호로 암호화된 비밀번호를 설정
                .username("newuser")
                .nickname("newuserNickname")
                .role(UserRoles.ROLE_CUSTOMER)
                .build();

        when(userRepository.findByUsernameAndDeletedAtIsNull("newuser")).thenReturn(Optional.of(newUser));
        when(passwordEncoder.matches("password", newUser.getPassword())).thenReturn(true);
        when(jwtUtil.createJwt(anyString(), anyString(), anyString(), any(UserRoles.class),1000L)).thenReturn("accessToken");

        // When
        JwtResponseDto response = userService.authenticateUser(loginRequestDto);

        // Then
        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void testAuthenticateUserFailPassword() {
        // Given
        LoginRequestDto loginRequestDto = new LoginRequestDto("testuser", "1234");

        when(userRepository.findByUsernameAndDeletedAtIsNull("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("1234", testUser.getPassword())).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.authenticateUser(loginRequestDto));
        assertEquals("Invalid password : 1234", exception.getMessage());
    }

    @Test
    @DisplayName("유저 단일 조회 성공")
    void testGetUserByIdSuccess() {
        // Given
        when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(testUser));

        // When
        UserResDto result = userService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
    }

    @Test
    @DisplayName("유저 단일 조회 실패 - 존재하지않는 ID(UUID)")
    void testGetUserByIdFail(){
        // Given
        when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());


        // When Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.getUserById(userId));
        assertEquals("User Not Found By Id : " + userId, exception.getMessage());
    }

    @Test
    @DisplayName("모든유저 조회 성공")
    void testGetUsersSuccess(){

        // Given
        UserSearchReqDto userSearchReqDto = new UserSearchReqDto();
        userSearchReqDto.setPage(0);
        userSearchReqDto.setSize(10);
        userSearchReqDto.setSortBy("createdAt");
        userSearchReqDto.setOrder("asc");

        // Given: 테스트용 유저 객체 생성
        User testUser = User.builder()
                .userId(UUID.randomUUID()) // UUID 생성
                .username("testUser")
                .password("password123") // 테스트에서는 암호화 없이 설정
                .email("testuser@example.com")
                .nickname("Test User")
                .role(UserRoles.ROLE_CUSTOMER) // 유저 권한 설정
                .deliveryAddresses(new ArrayList<>()) // 배송 주소 리스트 초기화
                .build();

        // Given: Mock으로 페이징된 유저 리스트 반환 설정
        Page<User> userPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(BooleanBuilder.class), any(Pageable.class))).thenReturn(userPage);

        // When: 서비스 메서드 실행
        Page<UserResDto> result = userService.getUsers(userSearchReqDto);

        // Then: 검증
        assertNotNull(result); // null이 아님을 확인
        assertEquals(1, result.getTotalElements()); // 총 개수가 1인지 확인
        assertEquals("testUser", result.getContent().get(0).getUsername()); // 첫 번째 유저의 username 검증

    }

    @Test
    @DisplayName("모든 유저 조회 실패 - 결과 없음")
    void testGetUsersFail(){
        // Given
        UserSearchReqDto userSearchReqDto = new UserSearchReqDto();
        userSearchReqDto.setPage(0);
        userSearchReqDto.setSize(10);
        userSearchReqDto.setSortBy("createdAt");
        userSearchReqDto.setOrder("asc");

        Page<User> emptyPage = Page.empty();
        when(userRepository.findAll(any(BooleanBuilder.class), any(Pageable.class))).thenReturn(emptyPage);


        // When & Then
        UserNotFoundException userNotFoundException = assertThrows(UserNotFoundException.class,
                () -> userService.getUsers(userSearchReqDto));
        assertEquals("Users Not Found", userNotFoundException.getMessage());
    }

    @Test
    @DisplayName("회원정보 수정 성공 테스트")
    void testUpdateUserSuccess(){

        // Given
        UserUpdateReqDto userUpdateReqDto = new UserUpdateReqDto("encodedPassword", "newPassword", "newEmail@example.com", "newNickname");

        // PrincipalDetails mock 객체 생성
        PrincipalDetails principalDetails = mock(PrincipalDetails.class);
        when(principalDetails.getUsername()).thenReturn("testuser");

        when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("encodedPassword", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User updateUser = testUser.toBuilder()
                .email("newEmail@example.com")
                .nickname("newNickname")
                .password("encodedNewPassword")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(updateUser);


        // When
        UserResDto result = userService.updateUser(userId, principalDetails, userUpdateReqDto);

        // Then
        assertNotNull(result);
        assertEquals("newEmail@example.com", result.getEmail());
        assertEquals("newNickname", result.getNickname());
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 권한 없음")
    void testUpdateUserFailAccessDenied(){
        // Given
        UserUpdateReqDto userUpdateReqDto = new UserUpdateReqDto("encodedPassword", "newPassword", "newEmail@example.com", "newNickname");

        // PrincipalDetails mock 객체 생성
        PrincipalDetails principalDetails = mock(PrincipalDetails.class);
        when(principalDetails.getUsername()).thenReturn("test"); // 다른 사용자로 설정
        when(principalDetails.getRole()).thenReturn(UserRoles.ROLE_CUSTOMER);

        // userRepository.findByUserIdAndDeletedAtIsNull()가 정상적으로 testUser 반환하도록 설정
        when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> userService.updateUser(userId, principalDetails, userUpdateReqDto));
        assertEquals("Access denied.", exception.getMessage());
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 비밀번호 불일치")
    void testUpdateUserFailIncorrectPassword(){
        // Given
        UserUpdateReqDto userUpdateReqDto = new UserUpdateReqDto("encodedPassword", "newPassword", "newEmail@example.com", "newNickname");

        // PrincipalDetails mock 객체 생성
        PrincipalDetails principalDetails = mock(PrincipalDetails.class);
        when(principalDetails.getUsername()).thenReturn("testuser");

        when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("encodedPassword", testUser.getPassword())).thenReturn(false);

        ForbiddenException exception = assertThrows(ForbiddenException.class, ()-> userService.updateUser(userId,principalDetails,userUpdateReqDto));
        assertEquals("Incorrect password.", exception.getMessage());
    }

    @Test
    @DisplayName("ROLE 변경 성공 - ROLE_MASTER 사용자")
    void testUpdateRoleSuccess(){
        // ROLE_CUSTOMER -> ROLE_MANAGER 로 변경

        // Given
        PrincipalDetails principalDetails = mock(PrincipalDetails.class);
        when(principalDetails.getRole()).thenReturn(UserRoles.ROLE_MASTER);
        when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(testUser));
        UserRoleUpdateReqDto reqDto = new UserRoleUpdateReqDto(UserRoles.ROLE_MANAGER);
        User updatedUser = testUser.toBuilder().role(reqDto.getRole()).build();
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // When
        UserResDto result = userService.updateRole(userId, principalDetails, reqDto);

        // Then
        assertNotNull(result);
        assertEquals(UserRoles.ROLE_MANAGER.name(), result.getRole());
    }

    @Test
    @DisplayName("role 변경 실패 - 권한 없음")
    void testUpdateRoleFail(){
        // ROLE_CUSTOMER -> ROLE_MANAGER 로 변경

        // Given
        PrincipalDetails principalDetails = mock(PrincipalDetails.class);
        when(principalDetails.getRole()).thenReturn(UserRoles.ROLE_CUSTOMER);
        UserRoleUpdateReqDto userRoleUpdateReqDto = new UserRoleUpdateReqDto(UserRoles.ROLE_MANAGER);

        // Then
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> userService.updateRole(userId, principalDetails ,userRoleUpdateReqDto));
        assertEquals("Access denied.", exception.getMessage());
    }


    @Test
    @DisplayName("User 삭제 성공 - 본인 계정")
    void testDeleteUserSuccess(){

        // Given
        PrincipalDetails principalDetails = mock(PrincipalDetails.class);
        when(principalDetails.getUsername()).thenReturn(testUser.getUsername());
        when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(testUser));


        // When & Then
        assertDoesNotThrow(() -> userService.deleteUser(userId,principalDetails));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 삭제 실패 - 권한 없음")
    void testDeleteUserFailForbidden() {
        // Given
        PrincipalDetails principalDetails = mock(PrincipalDetails.class);
        when(principalDetails.getUsername()).thenReturn("otherUser");
        when(principalDetails.getRole()).thenReturn(UserRoles.ROLE_CUSTOMER);
        when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> userService.deleteUser(userId, principalDetails));
        assertEquals("Access denied.", exception.getMessage());
    }
}
