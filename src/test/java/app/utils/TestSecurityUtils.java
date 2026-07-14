package app.utils;

import app.enums.SecurityRole;
import app.models.User;
import app.services.JwtService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

/**
 * Classe utilitaire pour les tests de sécurité.
 * Fournit des méthodes helper pour créer des utilisateurs et tokens de test.
 */
public class TestSecurityUtils {

    /**
     * Crée un utilisateur de test basique.
     */
    public static User createTestUser(String username, String password, SecurityRole role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    /**
     * Crée un UserDetails pour Spring Security.
     */
    public static UserDetails createUserDetails(String username, String password, SecurityRole role) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password(password)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role.name())))
                .build();
    }

    /**
     * Génère un token JWT pour un utilisateur.
     */
    public static String generateTokenForUser(JwtService jwtService, String username, SecurityRole role) {
        UserDetails userDetails = createUserDetails(username, "password", role);
        return jwtService.generateToken(userDetails);
    }

    /**
     * Crée un header Authorization Bearer complet.
     */
    public static String createAuthorizationHeader(String token) {
        return "Bearer " + token;
    }
}