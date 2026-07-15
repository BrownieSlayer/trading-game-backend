package app.services;

import java.util.List;

import app.dto.minigames.EmploymentDto;
import app.dto.minigames.JobOfferDto;
import app.models.User;

/**
 * Port de {@code minigames/emploi.py} : compétences à niveaux, offres
 * générées pondérées par le niveau total de compétences, poste actif crédité
 * une seule fois à échéance fixe (pas d'accrual continu, contrairement au
 * Business).
 */
public interface EmploymentService {

    /** Crédite d'abord le poste actif s'il est arrivé à échéance, puis renvoie l'état à jour. */
    EmploymentDto getStatus(User user);

    /** Génère de nouvelles offres (remplace les précédentes). Refuse si un poste est déjà en cours. */
    List<JobOfferDto> searchOffers(User user);

    EmploymentDto acceptOffer(User user, int offerIndex);

    /** {@code skill} : "manuel", "intellect" ou "informatique". */
    EmploymentDto upgradeSkill(User user, String skill);
}
