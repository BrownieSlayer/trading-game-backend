package app.dto.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un point de l'historique de valeur du portefeuille (un par jour joué). */
public record PortfolioValuePointDto(LocalDate date, BigDecimal value) {}
