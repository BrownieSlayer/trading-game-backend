package app.dto.portfolio;

import java.math.BigDecimal;
import java.util.List;

/** Vue d'ensemble d'un portefeuille : liquidités, valorisation totale, gain global, positions détaillées. */
public record PortfolioDto(
    BigDecimal cash,
    BigDecimal startingCapital,
    BigDecimal totalTaxesPaid,
    BigDecimal totalValue,
    BigDecimal gainAmount,
    double gainPercent,
    List<HoldingDto> holdings
) {}
