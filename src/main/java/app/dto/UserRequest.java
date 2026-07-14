package app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload utilisateur")
public record UserRequest(

    @Schema(description = "Pseudo de l'utilisateur", example = "Golden")
    @NotBlank(message = "Le pseudo est obligatoire") 
    @Size(min = 3, max = 150, message = "Le pseudo doit contenir entre 3 et 150 caractères")
    String username,

    @Schema(description = "Mot de passe de l'utilisateur", example = "password123")
    @NotBlank(message = "Le mot de passe est obligatoire") 
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    String password

) {}
