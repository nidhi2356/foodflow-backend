package com.foodflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodflow.dto.AuthRequest;
import com.foodflow.dto.AuthResponse;
import com.foodflow.dto.RegisterRequest;
import com.foodflow.exception.DuplicateResourceException;
import com.foodflow.security.JwtService;
import com.foodflow.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/auth/register should return 201 Created on valid request")
    void registerShouldReturnCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@foodflow.com")
                .password("password123")
                .fullName("New User")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("jwt_token_example")
                .type("Bearer")
                .userId(1L)
                .username("newuser")
                .email("new@foodflow.com")
                .role("ROLE_USER")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt_token_example"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("new@foodflow.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 Bad Request on invalid payload")
    void registerShouldReturnBadRequestOnValidationFailure() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("")
                .email("invalid-email")
                .password("123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register should return 409 Conflict when username exists")
    void registerShouldReturnConflictWhenDuplicate() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("existing@foodflow.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Username 'existinguser' is already taken"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'existinguser' is already taken"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 OK on valid credentials")
    void loginShouldReturnToken() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("jwt_token_example")
                .type("Bearer")
                .userId(1L)
                .username("testuser")
                .email("test@foodflow.com")
                .role("ROLE_USER")
                .build();

        when(authService.login(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token_example"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 401 Unauthorized on invalid credentials")
    void loginShouldReturnUnauthorizedOnBadCredentials() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
