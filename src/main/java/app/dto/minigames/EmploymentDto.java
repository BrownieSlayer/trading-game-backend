package app.dto.minigames;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * État du mini-jeu Emploi. {@code skills} est indexé par
 * "manuel"/"intellect"/"informatique". {@code currentOffers} est vide s'il
 * n'y a rien à proposer (aucune recherche faite, ou poste actif en cours —
 * {@code activeJob} est alors non-null et {@code currentOffers} vide).
 */
public record EmploymentDto(
    BigDecimal totalEarned,
    Map<String, UpgradeStatusDto> skills,
    List<JobOfferDto> currentOffers,
    ActiveJobDto activeJob
) {}
