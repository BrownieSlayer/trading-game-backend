package app.dto.security;

/**
 * DTO pour les réponses d'authentification.
 * 
 * @param token Token JWT généré
 */
public record AuthResponse(
    String username
) {}