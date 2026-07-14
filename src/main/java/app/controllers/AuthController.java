package app.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import app.dto.UserDto;
import app.dto.security.AuthRequest;
import app.dto.security.AuthResponse;
import app.dto.security.RegisterRequest;
import app.enums.SecurityRole;
import app.models.User;
import app.repositories.UserRepository;
import app.security.SimpleRateLimiter;
import app.services.JwtService;
import app.services.PortfolioService;
import app.services.UserDetailsServiceImpl;
import app.validators.PasswordValidator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Controller pour l'authentification et l'inscription.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${cookie.secure:true}")
    private boolean secureCookie; 

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final SimpleRateLimiter rateLimiter;
    private final PortfolioService portfolioService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserDetailsServiceImpl userDetailsService,
            SimpleRateLimiter rateLimiter,
            PortfolioService portfolioService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.rateLimiter = rateLimiter;
        this.portfolioService = portfolioService;
    }

    /**
     * Endpoint de connexion.
     * Authentifie l'utilisateur et retourne un token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
        @RequestBody AuthRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse response
    ) {

        String clientIp = getClientIP(httpRequest);
        
        if (!rateLimiter.allowRequest(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Too many requests"));
        }

        try {
            // Authentification
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            // Génération du token
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
            String token = jwtService.generateToken(userDetails);

            // Stockage du token dans un cookie httpOnly
            addJwtCookie(response, token);

             User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));



            return ResponseEntity.ok(new UserDto(
                user.getUsername(),
                user.getRole()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
        @RequestBody RegisterRequest request,
        HttpServletResponse response
    ) {
        try {
            // Vérification de la force du mot de passe
            PasswordValidator.validate(request.password());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        // Vérifie si l'utilisateur existe déjà
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nom d'utilisateur déjà utilisé"));
        }

        
        // Crée l'utilisateur
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(SecurityRole.ROLE_USER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        portfolioService.createPortfolioForUser(savedUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String token = jwtService.generateToken(userDetails);

        // Stockage du token dans un cookie httpOnly
        addJwtCookie(response, token);

        return ResponseEntity.ok(new UserDto(
            user.getUsername(),
            user.getRole()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        
        return ResponseEntity.ok().build();
    }

    private void addJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60);
        response.addCookie(cookie);
        cookie.setAttribute("SameSite", "Lax");
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    /**
     * Retourne les informations de l'utilisateur actuellement connecté.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String username = userDetails.getUsername();
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        
        return ResponseEntity.ok(new UserDto(
            user.getUsername(),
            user.getRole()
        ));
    }
}