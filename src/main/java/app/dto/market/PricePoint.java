package app.dto.market;

import java.time.LocalDate;

/** Un point de l'historique de cours d'un ticker (toujours en euros). */
public record PricePoint(LocalDate date, double price) {}
