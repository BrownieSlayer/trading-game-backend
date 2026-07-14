package app.dto.portfolio;

import java.math.BigDecimal;

/**
 * Une position détenue, avec sa valorisation courante. {@code currentPrice}
 * retombe sur {@code averageBuyPrice} si le cours en cache est indisponible
 * (voir {@code core/portfolio.py:calculate_holding_gain}).
 */
public record HoldingDto(
    String ticker,
    String name,
    BigDecimal quantity,
    BigDecimal averageBuyPrice,
    BigDecimal currentPrice,
    BigDecimal value,
    BigDecimal gainAmount,
    double gainPercent
) {}
