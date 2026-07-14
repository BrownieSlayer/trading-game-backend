package app.services;

import app.models.User;

/**
 * Résolution paresseuse du "nouveau jour" — portage de
 * {@code core/portfolio.py:is_new_day}/{@code update_streak}. Appelée au
 * premier accès authentifié de la journée à une route liée au portefeuille
 * (pas de job batch global) : met à jour le streak et enregistre un point
 * de valeur quotidien.
 */
public interface DailyCycleService {

    void resolveNewDay(User user);
}
