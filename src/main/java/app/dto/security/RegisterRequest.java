package app.dto.security;

/**
 * DTO pour les requêtes d'inscription.
 * 
 * @param name Nom d'affichage
 * @param username Nom d'utilisateur unique
 * @param password Mot de passe en clair (hashé à l'enregistrement)
 */
public record RegisterRequest(
    String username,
    String password,
    String gameName,
    String tag
) {}
