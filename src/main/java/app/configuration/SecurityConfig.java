package app.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import app.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * Configuration centrale de Spring Security.
 * 
 * <h2>Responsabilités principales :</h2>
 * <ul>
 *   <li>Définir la chaîne de filtres de sécurité</li>
 *   <li>Configurer l'authentification JWT</li>
 *   <li>Gérer les autorisations d'accès aux endpoints</li>
 *   <li>Configurer CORS pour les appels depuis le frontend</li>
 * </ul>
 * 
 * <h2>Auto-configuration Spring Security 6.3+ :</h2>
 * <p>Il suffit de définir {@link UserDetailsService} et {@link PasswordEncoder} comme beans.
 * Spring Security crée automatiquement un {@code DaoAuthenticationProvider} qui les utilise.
 * Plus besoin de le configurer manuellement !</p>
 */
@Configuration
@EnableWebSecurity  // Active Spring Security
@EnableMethodSecurity  // Permet @PreAuthorize, @Secured, etc.
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Configuration de la chaîne de filtres de sécurité.
     * 
     * <p>Cette méthode configure l'ensemble du pipeline de sécurité :</p>
     * 
     * <h3>1. CSRF (Cross-Site Request Forgery)</h3>
     * Désactivé car nous utilisons JWT (stateless). Le token JWT dans le header
     * Authorization protège contre CSRF. CSRF est utile uniquement pour les
     * sessions avec cookies.
     * 
     * <h3>2. CORS (Cross-Origin Resource Sharing)</h3>
     * Permet au frontend Angular (localhost:4200) d'appeler l'API backend.
     * Sans CORS, le navigateur bloque les requêtes cross-origin.
     * 
     * <h3>3. Autorisation des requêtes</h3>
     * <ul>
     *   <li>/api/auth/** → Accessible sans authentification (login, register)</li>
     *   <li>/api/public/** → Accessible sans authentification (endpoints publics)</li>
     *   <li>Tous les autres → Nécessitent un token JWT valide</li>
     * </ul>
     * 
     * <h3>4. Gestion des sessions</h3>
     * STATELESS : Aucune session HTTP n'est créée. Chaque requête est indépendante
     * et doit contenir le token JWT. Parfait pour les API REST.
     * 
     * <h3>5. Chaîne de filtres</h3>
     * Le JwtAuthenticationFilter est ajouté AVANT UsernamePasswordAuthenticationFilter.
     * Il intercepte chaque requête pour vérifier le token JWT avant que Spring Security
     * n'effectue l'authentification standard.
     * 
     * @param http Configuration HTTP Security
     * @return La chaîne de filtres configurée
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Désactiver CSRF (pas nécessaire avec JWT)
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. Configurer CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 3. Configurer les autorisations
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics (pas d'authentification requise)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                
                // Tous les autres endpoints nécessitent une authentification
                .anyRequest().authenticated()
            )
            
            // 4. Configuration stateless (pas de sessions)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 5. Gestion des exceptions autentification et d'accès
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN);
                })
            )
                
            // 5. Ajouter le filtre JWT avant le filtre d'authentification standard
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Expose l'AuthenticationManager pour l'utiliser dans les controllers.
     * 
     * <p>Utilisé dans AuthController pour authentifier les utilisateurs lors du login :</p>
     * <pre>
     * authenticationManager.authenticate(
     *     new UsernamePasswordAuthenticationToken(username, password)
     * );
     * </pre>
     * 
     * <p>Spring Security crée automatiquement un AuthenticationManager qui utilise
     * le UserDetailsService et le PasswordEncoder définis comme beans.</p>
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configure l'encodeur de mots de passe.
     * 
     * <p>BCrypt est un algorithme de hashing sécurisé :</p>
     * <ul>
     *   <li>Salt automatique (protection contre rainbow tables)</li>
     *   <li>Lent par conception (protection contre brute force)</li>
     *   <li>Coût configurable (par défaut : 10 rounds)</li>
     * </ul>
     * 
     * <p>Spring Security détecte automatiquement ce bean et l'utilise
     * pour encoder/vérifier les mots de passe.</p>
     * 
     * <p>Exemple de hash BCrypt :</p>
     * <pre>
     * Input:  "password123"
     * Output: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhka"
     *          ↑    ↑  ↑                    ↑
     *       algo cost salt                hash
     * </pre>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure CORS (Cross-Origin Resource Sharing).
     * 
     * <p>Permet au frontend Angular de faire des requêtes vers l'API backend.</p>
     * 
     * <h3>Configuration :</h3>
     * <ul>
     *   <li><b>allowedOrigins</b> : Liste des URLs autorisées (ici Angular sur localhost:4200)</li>
     *   <li><b>allowedMethods</b> : Méthodes HTTP autorisées (GET, POST, PUT, DELETE, etc.)</li>
     *   <li><b>allowedHeaders</b> : Tous les headers autorisés (notamment Authorization pour JWT)</li>
     *   <li><b>allowCredentials</b> : Permet l'envoi de cookies/credentials</li>
     * </ul>
     * 
     * <h3>Pourquoi CORS ?</h3>
     * <p>Par défaut, les navigateurs bloquent les requêtes cross-origin pour des raisons de sécurité.
     * CORS définit quelles origines sont autorisées à accéder à l'API.</p>
     * 
     * <h3>En production :</h3>
     * <pre>
     * configuration.setAllowedOrigins(List.of(
     *     "https://monapp.com",
     *     "https://www.monapp.com"
     * ));
     * </pre>
     * 
     * @return La source de configuration CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origins autorisées - EXPLICITES, jamais de wildcard avec credentials
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        
        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Headers EXPLICITES que le client peut envoyer
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With"
        ));
        
        // Headers que le client peut lire dans la réponse
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "X-Total-Count"
        ));
        
        // Permet l'envoi de credentials
        configuration.setAllowCredentials(true);
        
        // Cache des requêtes preflight pendant 1 heure
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}