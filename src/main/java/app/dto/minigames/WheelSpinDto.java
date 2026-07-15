package app.dto.minigames;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Résultat d'un tirage de la roue de la fortune. */
public record WheelSpinDto(String label, BigDecimal gain, LocalDate spinDate) {}
