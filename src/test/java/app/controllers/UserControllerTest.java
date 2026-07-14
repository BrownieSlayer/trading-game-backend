package app.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import app.configuration.TestcontainersConfiguration;
import app.dto.UserRequest;
import app.dto.UserDto;
import app.enums.SecurityRole;
import app.security.JwtAuthenticationFilter;
import app.services.JwtService;
import app.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestcontainersConfiguration.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(username = "john", roles = {"USER"})
    void testCreateUser() throws Exception {
        UserRequest userRequest = new UserRequest("John", "password123");
        UserDto userResponse = new UserDto(
            "John",
            SecurityRole.ROLE_USER,
            "JohnPlayer#EUW",
            "GOLD",
            "III"
        );

        when(userService.create(any(UserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("John"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.riotId").value("JohnPlayer#EUW"))
                .andExpect(jsonPath("$.rankTier").value("GOLD"))
                .andExpect(jsonPath("$.rankDivision").value("III"));
    }

    @Test
    @WithMockUser(username = "john", roles = {"USER"})
    void testGetUser() throws Exception {
        long userId = 1L;
        UserDto userResponse = new UserDto(
            "John",
            SecurityRole.ROLE_USER,
            "JohnPlayer#EUW",
            "GOLD",
            "III"
        );
        when(userService.getById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("John"))
                .andExpect(jsonPath("$.riotId").value("JohnPlayer#EUW"));
    }

    @Test
    @WithMockUser(username = "john", roles = {"USER"})
    void testGetUserNotFound() throws Exception {
        long userId = 999L;
        when(userService.getById(userId)).thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "john", roles = {"USER"})
    void testGetAllUsers() throws Exception {
        UserDto firstUserResponse = new UserDto(
            "John",
            SecurityRole.ROLE_USER,
            "JohnPlayer#EUW",
            "GOLD",
            "III"
        );
        UserDto secondUserResponse = new UserDto(
            "Alice",
            SecurityRole.ROLE_USER,
            "AlicePlayer#EUW",
            "PLATINUM",
            "II"
        );

        List<UserDto> users = Arrays.asList(firstUserResponse, secondUserResponse);
        when(userService.getAll()).thenReturn(users);

        mockMvc.perform(get("/api/users").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("John"))
                .andExpect(jsonPath("$[0].riotId").value("JohnPlayer#EUW"))
                .andExpect(jsonPath("$[1].username").value("Alice"))
                .andExpect(jsonPath("$[1].riotId").value("AlicePlayer#EUW"));
    }

    @Test
    @WithMockUser(username = "john", roles = {"USER"})
    void testGetDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
