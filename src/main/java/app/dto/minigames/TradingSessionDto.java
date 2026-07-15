package app.dto.minigames;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import app.enums.ReflexSessionStatus;

/**
 * Une partie de trading rapide. {@code prices} contient toute la marche
 * aléatoire des 25 ticks, générée une fois pour toutes à {@code start()} —
 * le client rejoue l'animation localement à partir de {@code startedAt},
 * il ne génère jamais lui-même les prix. {@code gain} est null tant que la
 * partie est {@code OPEN}.
 */
public record TradingSessionDto(
    Long sessionId,
    List<BigDecimal> prices,
    Instant startedAt,
    Integer buyTickIndex,
    Integer sellTickIndex,
    BigDecimal gain,
    boolean abandoned,
    ReflexSessionStatus status
) {}
