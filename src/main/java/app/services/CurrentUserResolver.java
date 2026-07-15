package app.services;

import org.springframework.security.core.userdetails.UserDetails;

import app.models.User;

/**
 * Résout l'entité {@link User} du principal authentifié et déclenche au
 * passage la résolution paresseuse du "nouveau jour" ({@link DailyCycleService})
 * — tout endpoint lié au portefeuille ou aux mini-jeux doit passer par ici
 * plutôt que de requêter {@code UserRepository} directement, pour que le
 * cycle quotidien se résolve au premier accès authentifié de la journée,
 * quelle que soit la route utilisée.
 */
public interface CurrentUserResolver {

    User resolve(UserDetails userDetails);
}
