package app.dto.minigames;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Montant en euros")
public record AmountRequest(

    @Schema(description = "Montant", example = "500")
    @NotNull(message = "Le montant est obligatoire")
    BigDecimal amount

) {}
