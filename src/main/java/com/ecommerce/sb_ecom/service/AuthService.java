package com.ecommerce.sb_ecom.service;

import com.ecommerce.sb_ecom.payload.AuthenticationResult;
import com.ecommerce.sb_ecom.payload.UserResponse;
import com.ecommerce.sb_ecom.security.request.LoginRequest;
import com.ecommerce.sb_ecom.security.request.SignupRequest;
import com.ecommerce.sb_ecom.security.response.UserInfoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;


public interface AuthService {
    AuthenticationResult login(LoginRequest loginRequest);

    ResponseEntity register(@Valid SignupRequest signupRequest);

    UserInfoResponse getCurrentUserDetails(Authentication authentication, HttpServletRequest request);

    ResponseCookie logoutUser();

    UserResponse getAllSellers(Pageable pageDetails);
}
