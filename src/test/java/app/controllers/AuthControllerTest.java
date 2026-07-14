package app.controllers;

import app.configuration.TestcontainersConfiguration;
import app.enums.SecurityRole;
import app.models.User;
import app.repositories.UserRepository;
import app.security.SimpleRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour AuthController.
 * Teste les endpoints d'authentification avec Spring Security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SimpleRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // Nettoyage de la base de test
        userRepository.deleteAll();
        rateLimiter.reset();

        // Création d'un utilisateur de test avec un LinkedAccount
        User testUser = new User();
        testUser.setUsername("john");
        testUser.setPassword(passwordEncoder.encode("StrongPassword123!"));
        testUser.setRole(SecurityRole.ROLE_USER);
        testUser.setEnabled(true);

        userRepository.save(testUser);
    }

    @Test
    void testShouldLoginSuccessfully() throws Exception {
        String loginJson = """
            {
                "username": "john",
                "password": "StrongPassword123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(cookie().exists("jwt"))
                .andExpect(cookie().httpOnly("jwt", true));
    }

    @Test
    void testShouldRejectLoginWithWrongPassword() throws Exception {
        String loginJson = """
            {
                "username": "john",
                "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("jwt"));
    }

    @Test
    void testShouldRejectLoginWithNonExistentUser() throws Exception {
        String loginJson = """
            {
                "username": "nonexistent",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testShouldRegisterNewUserSuccessfully() throws Exception {

        String registerJson = """
            {
                "username": "newuser",
                "password": "StrongPassword123!",
                "gameName": "NewPlayer",
                "tag": "EUW"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(cookie().exists("jwt"));

        // Vérification que l'utilisateur a bien été créé
        var user = userRepository.findByUsername("newuser");
        assert user.isPresent();
        assert user.get().getRole() == SecurityRole.ROLE_USER;
        assert user.get().isEnabled();
    }

    @Test
    void testShouldRejectRegistrationWithExistingUsername() throws Exception {
        String registerJson = """
            {
                "username": "john",
                "password": "StrongPassword123!",
                "gameName": "SomePlayer",
                "tag": "EUW"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Nom d'utilisateur déjà utilisé"));
    }

    @Test
    void testShouldRejectWeakPassword() throws Exception {
        String registerJson = """
            {
                "username": "weakuser",
                "password": "weak",
                "gameName": "Player",
                "tag": "EUW"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testShouldEncodePasswordOnRegistration() throws Exception {

        String registerJson = """
            {
                "username": "passwordtest",
                "password": "PlainPassword123!",
                "gameName": "TestPlayer",
                "tag": "EUW"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk());

        var user = userRepository.findByUsername("passwordtest");
        assert user.isPresent();
        assert !user.get().getPassword().equals("PlainPassword123!");
        assert passwordEncoder.matches("PlainPassword123!", user.get().getPassword());
    }

    @Test
    void testShouldLogoutAndClearCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"))
                .andExpect(cookie().maxAge("jwt", 0));
    }

    @Test
    void testShouldGetCurrentUserWhenAuthenticated() throws Exception {
        // Login pour obtenir un cookie JWT
        String loginJson = """
            {
                "username": "john",
                "password": "StrongPassword123!"
            }
            """;

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        var jwtCookie = result.getResponse().getCookie("jwt");

        // Utilisation du cookie pour accéder à /me
        mockMvc.perform(get("/api/auth/me")
                        .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void testShouldRejectUnauthenticatedAccessToMe() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
