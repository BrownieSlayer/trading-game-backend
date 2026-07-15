package app.dto.minigames;

import java.math.BigDecimal;
import java.time.Instant;

public record ActiveJobDto(
    String title,
    String tier,
    int durationHours,
    Instant startedAt,
    Instant endsAt,
    BigDecimal hourlyWage,
    BigDecimal totalWage,
    int minutesRemaining
) {}
