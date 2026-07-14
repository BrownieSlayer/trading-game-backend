package app.integration;

import app.configuration.TestcontainersConfiguration;
import app.enums.SecurityRole;
import app.models.User;
import app.repositories.UserRepository;
import app.security.SimpleRateLimiter;
import app.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration pour la sécurité globale.
 * Vérifie que les endpoints sont correctement protégés.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SimpleRateLimiter rateLimiter;

    private String validToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Nettoyer la base
        userRepository.deleteAll();
        rateLimiter.reset();

        // Créer un utilisateur de test
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRole(SecurityRole.ROLE_USER);
        testUser.setEnabled(true);
        userRepository.save(testUser);

        // Générer un token valide
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getUsername())
                .password(testUser.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        validToken = jwtService.generateToken(userDetails);
    }

    // ========== Tests des endpoints publics ==========

    @Test
    void testShouldAllowAccessToPublicEndpoints() throws Exception {
        mockMvc.perform(get("/api/public/test"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testShouldAllowAccessToAuthEndpoints() throws Exception {
        String loginJson = """
            {
                "username": "testuser",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk());
    }

    // ========== Tests JWT ==========

    @Test
    void testShouldAllowAccessWithValidJwtToken() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void testShouldRejectAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testShouldRejectAccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testShouldRejectAccessWithMalformedAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "InvalidFormat " + validToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testShouldRejectAccessWithExpiredToken() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(
            jwtService, "jwtExpiration", 1L
        );

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getUsername())
                .password(testUser.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        
        String expiredToken = jwtService.generateToken(userDetails);

        // Attendre que le token expire
        Thread.sleep(10);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());

        org.springframework.test.util.ReflectionTestUtils.setField(
            jwtService, "jwtExpiration", 20000L
        );
    }

    // ========== Tests de rôles (si tu utilises @PreAuthorize) ==========

    @Test
    void testShouldAllowAccessForUserWithCorrectRole() throws Exception {
        // Test avec annotation @PreAuthorize("hasRole('ROLE_USER')")
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void testSshouldRejectAccessForUserWithInsufficientRole() throws Exception {
        mockMvc.perform(post("/api/users/testuser/disable")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden());
    }

    // ========== Test CSRF désactivé ==========

    @Test
    void testShouldNotRequireCsrfTokenForPostRequests() throws Exception {
        // CSRF est désactivé, donc POST devrait fonctionner sans token CSRF
        String loginJson = """
            {
                "username": "testuser",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk());
    }

    // ========== Test session STATELESS ==========

    @Test
    void testShouldNotCreateHttpSession() throws Exception {
        String loginJson = """
            {
                "username": "testuser",
                "password": "password123"
            }
            """;

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        // Vérifier qu'aucune session n'est créée
        assert result.getRequest().getSession(false) == null;
    }
}