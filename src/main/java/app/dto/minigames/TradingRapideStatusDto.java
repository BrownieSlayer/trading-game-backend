package app.dto.minigames;

/** {@code session} est null s'il n'y a aucune partie en cours. */
public record TradingRapideStatusDto(TradingSessionDto session, int attemptsRemainingToday) {}
