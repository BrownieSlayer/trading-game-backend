package app.models;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Valeur totale d'un portefeuille à la fin d'un jour joué — un point par jour, pour le graphique d'évolution. */
@Entity
@Table(
    name = "portfolio_value_snapshots",
    uniqueConstraints = @UniqueConstraint(name = "uq_snapshots_portfolio_date", columnNames = {"portfolio_id", "snapshot_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioValueSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalValue;
}
