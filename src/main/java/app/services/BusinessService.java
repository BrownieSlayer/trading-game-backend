package app.services;

import java.math.BigDecimal;

import app.dto.minigames.BusinessDto;
import app.models.User;

/**
 * Port de {@code minigames/business.py} : revenu passif crédité heure par
 * heure (aléa indépendant à chaque heure), récolte hors-ligne plafonnée,
 * calculée à la lecture — pas de job planifié par utilisateur.
 */
public interface BusinessService {

    /** Récolte d'abord les gains en attente, puis renvoie l'état à jour. */
    BusinessDto getStatus(User user);

    BusinessDto invest(User user, BigDecimal amount);

    BusinessDto withdraw(User user, BigDecimal amount);

    /** {@code upgradeType} : "capacite", "rendement" ou "stockage". */
    BusinessDto upgrade(User user, String upgradeType);
}
