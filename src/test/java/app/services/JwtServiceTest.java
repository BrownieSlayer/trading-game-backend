package app.services;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour JwtService.
 * Vérifie la génération, validation et extraction des tokens JWT.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secretKey", 
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L); // 24h

        userDetails = User.builder()
                .username("john")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    void testShouldGenerateToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT a 3 parties : header.payload.signature
    }

    @Test
    void testShouldExtractUsernameFromToken() {
        String token = jwtService.generateToken(userDetails);       
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("john");
    }

    @Test
    void testShouldValidateTokenSuccessfully() {
        String token = jwtService.generateToken(userDetails);
        boolean isValid = jwtService.isTokenValid(token, userDetails);
        assertThat(isValid).isTrue();
    }

    @Test
    void testShouldRejectTokenWithWrongUsername() {
        String token = jwtService.generateToken(userDetails);
        UserDetails differentUser = User.builder()
                .username("alice")
                .password("password")
                .authorities(Collections.emptyList())
                .build();


        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertThat(isValid).isFalse();
    }

    @Test
    void testShouldRejectExpiredToken() {
        // Given - Token avec expiration très courte (1ms)
        JwtService shortLivedJwtService = new JwtService();
        ReflectionTestUtils.setField(shortLivedJwtService, "secretKey", 
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(shortLivedJwtService, "jwtExpiration", 1L);

        String token = shortLivedJwtService.generateToken(userDetails);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean isValid = shortLivedJwtService.isTokenValid(token, userDetails);

        assertThat(isValid).isFalse();
    }

    @Test
    void testShouldExtractExpirationDate() {
        String token = jwtService.generateToken(userDetails);

        Claims claims = jwtService.extractClaim(token, claims1 -> claims1);

        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(new java.util.Date());
    }
}