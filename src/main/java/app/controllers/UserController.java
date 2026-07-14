package app.controllers;

import app.dto.*;
import app.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "API de gestion des utilisateurs")
public class UserController {

    private final UserService userService;

    @Operation(
        summary = "Créer un utilisateur",
        description = "Permet de créer un utilisateur avec nom et email."
    )
    @ApiResponse(responseCode = "201", description = "Utilisateur créé")
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserRequest request) {
        return ResponseEntity.status(201).body(userService.create(request));
    }

    @Operation(
        summary = "Obtenir un utilisateur par ID",
        description = "Retourne les informations publiques d’un utilisateur."
    )
    @ApiResponse(responseCode = "200", description = "Utilisateur trouvé")
    @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable @NotNull long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @Operation(
        summary = "Lister tous les utilisateurs",
        description = "Retourne l’ensemble des utilisateurs enregistrés."
    )
    @ApiResponse(responseCode = "200", description = "Liste renvoyée")
    @GetMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAll());
    }

    @Operation(
        summary = "Supprimer un utilisateur",
        description = "Supprime un utilisateur par ID."
    )
    @ApiResponse(responseCode = "204", description = "Utilisateur supprimé")
    @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Désactive un utilisateur",
        description = "Désactive un utilisateur par son username."
    )
    @ApiResponse(responseCode = "204", description = "Utilisateur désactivé")
    @ApiResponse(responseCode = "403", description = "Demandeur non autorisé")
    @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{username}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable String username) {
        userService.toggle(username, false);
        return ResponseEntity.noContent().build();
    }
}
