package app.controllers;

import app.configuration.TestcontainersConfiguration;
import app.enums.SecurityRole;
import app.models.LinkedAccount;
import app.models.User;
import app.repositories.UserRepository;
import app.services.impl.LinkedAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
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

    @MockitoBean
    private LinkedAccountService linkedAccountService;

    private User testUser;
    private LinkedAccount testLinkedAccount;

    @BeforeEach
    void setUp() {
        // Nettoyage de la base de test
        userRepository.deleteAll();

        // Création d'un utilisateur de test avec un LinkedAccount
        testUser = new User();
        testUser.setUsername("john");
        testUser.setPassword(passwordEncoder.encode("StrongPassword123!"));
        testUser.setRole(SecurityRole.ROLE_USER);
        testUser.setEnabled(true);

        testLinkedAccount = new LinkedAccount();
        testLinkedAccount.setRiotId("TestPlayer#EUW");
        testLinkedAccount.setPuuid("test-puuid-123");
        testLinkedAccount.setRankTier("GOLD");
        testLinkedAccount.setRankDivision("II");
        testLinkedAccount.setUser(testUser);

        testUser.setLinkedAccount(testLinkedAccount);

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
                .andExpect(jsonPath("$.riotId").value("TestPlayer#EUW"))
                .andExpect(jsonPath("$.rankTier").value("GOLD"))
                .andExpect(jsonPath("$.rankDivision").value("II"))
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
        // Mock du service pour simuler la création réussie du LinkedAccount
        LinkedAccount mockLinkedAccount = new LinkedAccount();
        mockLinkedAccount.setRiotId("NewPlayer#EUW");
        mockLinkedAccount.setPuuid("mock-puuid");
        mockLinkedAccount.setRankTier("SILVER");
        mockLinkedAccount.setRankDivision("III");

        when(linkedAccountService.createAndPopulateLinkedAccount(anyString(), anyString(), any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(2);
                    mockLinkedAccount.setUser(user);
                    user.setLinkedAccount(mockLinkedAccount);
                    return mockLinkedAccount;
                });

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
    void testShouldRejectRegistrationWithInvalidRiotAccount() throws Exception {
        // Mock du service pour simuler un compte Riot invalide
        doThrow(new RuntimeException("Compte Riot introuvable"))
                .when(linkedAccountService)
                .createAndPopulateLinkedAccount(anyString(), anyString(), any(User.class));

        String registerJson = """
            {
                "username": "failuser",
                "password": "StrongPassword123!",
                "gameName": "InvalidPlayer",
                "tag": "XXX"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Compte Riot introuvable ou invalide"));

        // Vérification que l'utilisateur n'a pas été créé (rollback)
        var user = userRepository.findByUsername("failuser");
        assert user.isEmpty();
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
        LinkedAccount mockLinkedAccount = new LinkedAccount();
        mockLinkedAccount.setRiotId("TestPlayer#EUW");
        mockLinkedAccount.setPuuid("mock-puuid");
        mockLinkedAccount.setRankTier("GOLD");
        mockLinkedAccount.setRankDivision("IV");

        when(linkedAccountService.createAndPopulateLinkedAccount(anyString(), anyString(), any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(2);
                    mockLinkedAccount.setUser(user);
                    user.setLinkedAccount(mockLinkedAccount);
                    return mockLinkedAccount;
                });

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
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.riotId").value("TestPlayer#EUW"))
                .andExpect(jsonPath("$.rankTier").value("GOLD"))
                .andExpect(jsonPath("$.rankDivision").value("II"));
    }

    @Test
    void testShouldRejectUnauthenticatedAccessToMe() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testShouldHandleUserWithoutLinkedAccountInLogin() throws Exception {
        // Création d'un utilisateur sans LinkedAccount
        User userWithoutLinkedAccount = new User();
        userWithoutLinkedAccount.setUsername("nolinked");
        userWithoutLinkedAccount.setPassword(passwordEncoder.encode("StrongPassword123!"));
        userWithoutLinkedAccount.setRole(SecurityRole.ROLE_USER);
        userWithoutLinkedAccount.setEnabled(true);
        userRepository.save(userWithoutLinkedAccount);

        String loginJson = """
            {
                "username": "nolinked",
                "password": "StrongPassword123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("nolinked"))
                .andExpect(jsonPath("$.riotId").value("Unknown#EUW"))
                .andExpect(jsonPath("$.rankTier").value(""))
                .andExpect(jsonPath("$.rankDivision").value("Unranked"));
    }
}
