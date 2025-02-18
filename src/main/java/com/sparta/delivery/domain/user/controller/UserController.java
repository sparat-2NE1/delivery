package com.sparta.delivery.domain.user.controller;

import com.sparta.delivery.config.auth.PrincipalDetails;
import com.sparta.delivery.domain.user.dto.*;
import com.sparta.delivery.domain.user.enums.UserRoles;
import com.sparta.delivery.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid SignupReqDto signupReqDto, BindingResult bindingResult){

        if (bindingResult.hasErrors()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ValidationErrorResponse(bindingResult));
        }

        return  ResponseEntity.status(HttpStatus.OK)
                .body(userService.signup(signupReqDto));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser (@RequestBody @Valid LoginRequestDto loginRequestDto, BindingResult bindingResult){

        if (bindingResult.hasErrors()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ValidationErrorResponse(bindingResult));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.authenticateUser(loginRequestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id")UUID id){
        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<?> getUsers(@RequestBody UserSearchReqDto userSearchReqDto){

        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.getUsers(userSearchReqDto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") UUID id,
                                        @AuthenticationPrincipal PrincipalDetails principalDetails,
                                        @RequestBody @Valid UserUpdateReqDto userUpdateReqDto,
                                        BindingResult bindingResult){

        if (bindingResult.hasErrors()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ValidationErrorResponse(bindingResult));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.updateUser(id, principalDetails, userUpdateReqDto));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable("id") UUID id,
                                        @AuthenticationPrincipal PrincipalDetails principalDetails,
                                        @RequestBody @Valid UserRoleUpdateReqDto userRoleUpdateReqDto,
                                        BindingResult bindingResult){

        if (bindingResult.hasErrors()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ValidationErrorResponse(bindingResult));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.updateRole(id, principalDetails, userRoleUpdateReqDto));
    }

    @PatchMapping("/{id}/delete")
    public ResponseEntity<?> deleteUser(@PathVariable("id") UUID id,
                                        @AuthenticationPrincipal PrincipalDetails principalDetails) {

        userService.deleteUser(id, principalDetails);

        return ResponseEntity.status(HttpStatus.OK)
                .build();
    }

    private Map<String, Object> ValidationErrorResponse(BindingResult bindingResult) {
        List<Map<String, String>> errors = bindingResult.getFieldErrors().stream()
                .map(fieldError -> Map.of(
                        "field", fieldError.getField(),
                        "message", fieldError.getDefaultMessage(),
                        "rejectedValue", String.valueOf(fieldError.getRejectedValue()) // 입력된 값도 포함
                ))
                .toList();

        return Map.of(
                "status", 400,
                "error", "Validation Field",
                "message", errors
        );
    }
}
