package app.dto.minigames;

import app.enums.ReflexAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Action du joueur pendant une partie de trading rapide")
public record ReflexActionRequest(

    @Schema(description = "Acheter, vendre, ou abandonner la partie en cours")
    @NotNull(message = "L'action est obligatoire")
    ReflexAction action

) {}
