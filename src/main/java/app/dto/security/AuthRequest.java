package app.dto.security;


/**
 * DTO pour les requêtes de connexion.
 * 
 * @param username Nom d'utilisateur
 * @param password Mot de passe
 */
public record AuthRequest(
    String username,
    String password
) {}