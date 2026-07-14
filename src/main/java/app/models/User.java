package app.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.CreationTimestamp;

import app.enums.SecurityRole;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Représente un utilisateur authentifié de l'application (compte de jeu).
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_username", columnNames = "username")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {

    /**
     * Identifiant unique de l'utilisateur.
     * Généré automatiquement par la base de données.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom d'utilisateur unique utilisé pour l'authentification.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Date et heure de création du compte utilisateur.
     * Générée automatiquement lors de la première insertion.
     * Ne peut pas être modifiée après création.
     */
    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    /**
     * Mot de passe hashé pour l'authentification.
     */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SecurityRole role = SecurityRole.ROLE_USER;

    /**
     * Indique si le compte est actif
     */
    @Column(nullable = false)
    private boolean enabled = true;
}