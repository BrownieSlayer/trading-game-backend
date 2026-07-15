package app.dto.minigames;

import java.math.BigDecimal;
import java.time.LocalDate;

import app.enums.BetDirection;
import app.enums.BetStatus;

/** Un pari du jour, actif ou résolu. {@code resultPrice}/{@code gain}/{@code resolvedDate} sont null tant qu'il est actif. */
public record BetDto(
    String ticker,
    BetDirection direction,
    BigDecimal stake,
    BigDecimal referencePrice,
    LocalDate placedDate,
    BetStatus status,
    BigDecimal resultPrice,
    BigDecimal gain,
    LocalDate resolvedDate
) {}
