package app.dto.minigames;

import java.math.BigDecimal;

/** {@code index} référence cette offre pour l'accepter (voir POST .../accept/{index}). */
public record JobOfferDto(int index, String title, String tier, int durationHours, BigDecimal hourlyWage, BigDecimal totalWage) {}
