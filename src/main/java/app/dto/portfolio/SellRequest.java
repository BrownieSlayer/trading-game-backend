package app.dto.portfolio;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Ordre de vente")
public record SellRequest(

    @Schema(description = "Ticker de la valeur", example = "AAPL")
    @NotBlank(message = "Le ticker est obligatoire")
    String ticker,

    @Schema(description = "Quantité à vendre", example = "10")
    @NotNull(message = "La quantité est obligatoire")
    BigDecimal quantity

) {}
