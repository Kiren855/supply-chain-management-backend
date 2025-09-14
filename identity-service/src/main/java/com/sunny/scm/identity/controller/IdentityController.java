package com.sunny.scm.identity.controller;

import com.sunny.scm.common.dto.ApiResponse;
import com.sunny.scm.identity.constant.IdentitySuccessCode;
import com.sunny.scm.identity.dto.auth.*;
import com.sunny.scm.identity.service.IdentityService;
import com.sunny.scm.identity.service.RoleService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RequiredArgsConstructor
@RestController
@RequestMapping("/identity/api/v1/auth")
@SuppressWarnings("unused")
public class IdentityController {
    private final IdentityService identityRootService;

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<?>> hello() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String companyId = jwt.getClaimAsString("company_id");

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .message("AC")
                .result(authentication.getAuthorities()).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("root/register")
    public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody RegisterRootRequest request) {
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .message(identityRootService.register(request)).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("sub/register")
    public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody RegisterSubRequest request) {
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .message(identityRootService.register(request)).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRootRequest request,
                                   HttpServletResponse response) {
        // get access token
        TokenExchangeResponse tokenResponse = identityRootService.login(request);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", tokenResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(IdentitySuccessCode.LOGIN_SUCCESS.getCode())
                .message(IdentitySuccessCode.LOGIN_SUCCESS.getMessage())
                .result(TokenExchangeResponse.builder()
                        .accessToken(tokenResponse.getAccessToken())
                        .expiresIn(tokenResponse.getExpiresIn())
                        .tokenType(tokenResponse.getTokenType())
                        .build())
                .build();

        return ResponseEntity
        .status(IdentitySuccessCode.LOGIN_SUCCESS.getHttpStatus())
        .body(apiResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> exchangeToken(@CookieValue(name = "refresh_token") String refreshToken,
     HttpServletResponse response)
     {
        TokenExchangeResponse tokenResponse = identityRootService.refreshToken(refreshToken);
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", tokenResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(IdentitySuccessCode.REFRESH_TOKEN_SUCCESS.getCode())
                .message(IdentitySuccessCode.REFRESH_TOKEN_SUCCESS.getMessage())
                .result(TokenExchangeResponse.builder()
                        .accessToken(tokenResponse.getAccessToken())
                        .tokenType(tokenResponse.getTokenType())
                        .expiresIn(tokenResponse.getExpiresIn())
                        .build())
                .build();

        return ResponseEntity
        .status(IdentitySuccessCode.REFRESH_TOKEN_SUCCESS.getHttpStatus())
        .body(apiResponse);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changeRootPassword(@Valid @RequestBody ChangePasswordRequest request) {
        identityRootService.changeRootPassword(request);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(IdentitySuccessCode.CHANGE_PASSWORD_SUCCESS.getCode())
                .message(IdentitySuccessCode.CHANGE_PASSWORD_SUCCESS.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @PostMapping("/{userId}/change-password")
    public ResponseEntity<?> changeSubPassword(
    @PathVariable String userId,
    @Valid @RequestBody ChangePasswordRequest request) {
        identityRootService.changeSubPassword(userId, request);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(IdentitySuccessCode.CHANGE_PASSWORD_SUCCESS.getCode())
                .message(IdentitySuccessCode.CHANGE_PASSWORD_SUCCESS.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refresh_token") String refreshToken,
        HttpServletResponse response)
    {
        identityRootService.logout(refreshToken);

        ResponseCookie deleteCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(IdentitySuccessCode.LOGOUT_SUCCESS.getCode())
                .message(IdentitySuccessCode.LOGOUT_SUCCESS.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

}
