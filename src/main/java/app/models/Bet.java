package app.models;

import java.math.BigDecimal;
import java.time.LocalDate;

import app.enums.BetDirection;
import app.enums.BetStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pari du jour — port de {@code minigames/pari.py}. Une seule ligne par
 * pari : {@code ACTIVE} tant qu'il n'est pas résolu, puis {@code WON}/
 * {@code LOST} en place (pas de table d'historique séparée, contrairement
 * au jeu Python — la ligne résolue EST l'historique).
 */
@Entity
@Table(name = "bet_pari")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BetDirection direction;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal stake;

    @Column(name = "reference_price", nullable = false, precision = 14, scale = 4)
    private BigDecimal referencePrice;

    @Column(name = "placed_date", nullable = false)
    private LocalDate placedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BetStatus status = BetStatus.ACTIVE;

    @Column(name = "result_price", precision = 14, scale = 4)
    private BigDecimal resultPrice;

    @Column(precision = 14, scale = 2)
    private BigDecimal gain;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;
}
