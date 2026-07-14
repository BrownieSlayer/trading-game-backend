package app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import app.services.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;

import java.io.IOException;

/**
 * Filtre qui intercepte chaque requête pour vérifier et valider le token JWT.
 * Extrait le token du header Authorization et authentifie l'utilisateur.
 * 
 * <h3>Flux d'exécution :</h3>
 * <ol>
 *   <li>Vérifie la présence du header Authorization avec "Bearer "</li>
 *   <li>Extrait et valide le token JWT</li>
 *   <li>Charge les détails de l'utilisateur depuis la base de données</li>
 *   <li>Place l'authentification dans le SecurityContext si valide</li>
 *   <li>Laisse passer la requête (Spring Security gère le refus si non authentifié)</li>
 * </ol>
 * 
 * <h3>Gestion des erreurs :</h3>
 * <p>Les erreurs de token sont loggées mais ne bloquent pas le filtre.
 * Si l'authentification échoue, le SecurityContext reste vide et Spring Security
 * déclenche l'AuthenticationEntryPoint configuré (qui renvoie une 401).</p>
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String jwt = null;
        
        // 1. Essayer de récupérer depuis le header Authorization
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        }
        
        // 2. Si pas trouvé, chercher dans les cookies
        if (jwt == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        // Pas de token = endpoints publics ou erreur 401
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("Successfully authenticated user: {} for URI: {}", username, request.getRequestURI());
                } else {
                    log.warn("Invalid JWT token for user: {}", username);
                }
            }
            
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {} - User: {}", e.getMessage(), e.getClaims().getSubject());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (UsernameNotFoundException e) {
            log.warn("User not found in database: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication: ", e);
        }

        filterChain.doFilter(request, response);
    }
}