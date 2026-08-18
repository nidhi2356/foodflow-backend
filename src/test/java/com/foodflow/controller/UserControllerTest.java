package com.foodflow.controller;

import com.foodflow.dto.UserResponse;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.security.JwtService;
import com.foodflow.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/users/me should return current user profile without password")
    void getCurrentUserShouldReturnProfile() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@foodflow.com")
                .fullName("Test User")
                .role("ROLE_USER")
                .build();

        when(userService.getCurrentUserProfile()).thenReturn(response);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@foodflow.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return user by ID without password")
    void getUserByIdShouldReturnUser() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(2L)
                .username("chef_john")
                .email("john@foodflow.com")
                .role("ROLE_USER")
                .build();

        when(userService.getUserById(2L)).thenReturn(response);

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.username").value("chef_john"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/users/{id} should return 404 when user not found")
    void getUserByIdShouldReturn404WhenNotFound() throws Exception {
        when(userService.getUserById(99L))
                .thenThrow(new ResourceNotFoundException("User", "id", 99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
