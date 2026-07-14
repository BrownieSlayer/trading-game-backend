package app.dto.portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import app.enums.TransactionType;

/** Un achat ou une vente exécuté. {@code capitalGain}/{@code tax} sont null pour un achat. */
public record TransactionDto(
    String ticker,
    TransactionType type,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal capitalGain,
    BigDecimal tax,
    LocalDateTime executedAt
) {}
