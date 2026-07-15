package app.dto.minigames;

import java.math.BigDecimal;

/** {@code nextCost} est null si le niveau maximum est déjà atteint. */
public record UpgradeStatusDto(String label, int level, int maxLevel, BigDecimal nextCost) {}
