package app.dto;

import app.enums.SecurityRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informations retournées pour un utilisateur")
public record UserDto(

    @Schema(description = "Username de l'utilisateur", example = "Golden")
    String username,

    @Schema(description = "Role l'utilisateur", example = "user")
    SecurityRole role

) {}