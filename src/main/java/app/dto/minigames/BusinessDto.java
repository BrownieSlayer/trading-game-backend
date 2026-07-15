package app.dto.minigames;

import java.math.BigDecimal;
import java.util.Map;

/**
 * État du mini-jeu Business. {@code estimatedHourlyYield} est une moyenne
 * théorique pour l'affichage (≈ X €/h) — la récolte réelle applique l'aléa
 * heure par heure, voir {@code minutesUntilNextHarvest} (null si aucun
 * investissement/récolte encore enregistrée). {@code upgrades} est indexé
 * par "capacite"/"rendement"/"stockage".
 */
public record BusinessDto(
    BigDecimal investment,
    BigDecimal estimatedHourlyYield,
    BigDecimal investmentCap,
    Integer minutesUntilNextHarvest,
    BigDecimal totalHarvested,
    Map<String, UpgradeStatusDto> upgrades
) {}
