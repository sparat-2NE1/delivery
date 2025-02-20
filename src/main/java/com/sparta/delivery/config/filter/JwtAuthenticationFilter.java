package com.sparta.delivery.config.filter;

import com.sparta.delivery.config.auth.PrincipalDetails;
import com.sparta.delivery.config.global.exception.custom.InvalidTokenException;
import com.sparta.delivery.domain.token.service.JwtUtil;
import com.sparta.delivery.domain.user.entity.User;
import com.sparta.delivery.domain.user.enums.UserRoles;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * 모든 Http 요청에서 JWT 토큰을 추철하고 사용자 인증을 처리하는 필터
 *
 * 1. 요청 헤더에서 JWT 토큰을 추출해 인즈 정보 설정
 * 2. 인증되지 않은 요청인 특정 URL은 필터 통과
 * 3. JWT 만료, 형식 오류, 서명 오류 등을 처리하고, 예외처리 코드와 메시지 반환
 *
 *
 * 특정 ULR (회원가입, 로그인 , 스웨거)은 필터에서 제외 (추후 권한 설정시 수정할듯)
 */

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Header key 식별값
    public static final String AUTHORIZATION_HEADER = "Authorization";

    // Token 식별자
    public static final String BEARER_PREFIX = "Bearer ";


    // 검사에서 제외할 경로
    private static final List<String> excludeUrls = List.of(
            "/api/user/signup",
            "/api/user/signin",
            "/api/token/reissue",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    );

    // 특정 URL이 해당하면 필터링을 저적용하지않도록 검사
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        return excludeUrls.stream().anyMatch(url -> pathMatcher.match(url, requestPath));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerAuthorizationToken = request.getHeader(AUTHORIZATION_HEADER);

        // JWT 토큰이 없거나 Bearer 접두어가 없는 경우
        if (headerAuthorizationToken == null || !headerAuthorizationToken.startsWith(BEARER_PREFIX)){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            PrintWriter writer = response.getWriter();
            writer.print("No authentication information found. Please log in.");
            return;
        }

        String accessToken = headerAuthorizationToken.split(" ")[1];

        try {
            jwtUtil.isExpired(accessToken);

            if (!jwtUtil.getCategory(accessToken).equals("access")){
                throw new InvalidTokenException("Invalid token category. Expected 'access' token.");
            }

            String username = jwtUtil.getUsername(accessToken);
            String email = jwtUtil.getEmail(accessToken);
            String role = jwtUtil.getRole(accessToken);

            User user = User.builder()
                    .username(username)
                    .email(email)
                    .role(UserRoles.fromString(role))
                    .build();

            // 인증 사용자 정보 생성
            PrincipalDetails principalDetails = new PrincipalDetails(user);

            // 스프링 시큐리티 인증 토큰 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails,
                    null,
                    principalDetails.getAuthorities());

            // 인증 정보 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException e) {
            PrintWriter writer = response.getWriter();
            writer.print("Access token expired 22");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (MalformedJwtException e) {
            PrintWriter writer = response.getWriter();
            writer.print("Malformed JWT token");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
            return;
        } catch (JwtException e) {
            PrintWriter writer = response.getWriter();
            writer.print("Invalid token claims");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
            return;
        }   catch (Exception e) {
            PrintWriter writer = response.getWriter();
            writer.print("Error processing JWT token");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
            return;
        }

        filterChain.doFilter(request, response);
    }
}
