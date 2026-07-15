package app.dto.leaderboard;

import java.math.BigDecimal;

/** Un compte dans le classement global, trié par performance (% de gain depuis le capital de départ). */
public record LeaderboardEntryDto(int rank, String username, BigDecimal totalValue, BigDecimal gainAmount, double gainPercent) {}
