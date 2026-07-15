package app.dto.minigames;

import java.math.BigDecimal;

import app.enums.BetDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Pari du jour sur la hausse ou la baisse d'un ticker avant la prochaine session")
public record PlaceBetRequest(

    @Schema(description = "Ticker de la valeur", example = "AAPL")
    @NotBlank(message = "Le ticker est obligatoire")
    String ticker,

    @Schema(description = "Sens du pari")
    @NotNull(message = "Le sens du pari est obligatoire")
    BetDirection direction,

    @Schema(description = "Mise en euros (mise par défaut du jeu si non précisée)", example = "50")
    BigDecimal stake

) {}
