package app.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Portefeuille de paper-trading d'un utilisateur (relation 1:1 avec {@link User}). */
@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal cash;

    @Column(name = "starting_capital", nullable = false, precision = 14, scale = 2)
    private BigDecimal startingCapital;

    @Column(name = "total_taxes_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTaxesPaid = BigDecimal.ZERO;

    /** Date de la dernière session jouée ; null pour un portefeuille tout neuf. Mis à jour par le cycle quotidien (Phase 3). */
    @Column(name = "last_session_date")
    private LocalDate lastSessionDate;

    /** Streak de jours joués consécutifs. Mis à jour par le cycle quotidien (Phase 3). */
    @Column(nullable = false)
    private int streak = 0;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;
}
