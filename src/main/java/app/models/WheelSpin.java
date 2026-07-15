package app.models;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un tirage de la roue de la fortune — port de {@code minigames/roue.py}. Au plus un par portefeuille et par jour. */
@Entity
@Table(
    name = "wheel_spins",
    uniqueConstraints = @UniqueConstraint(name = "uq_wheel_spins_portfolio_date", columnNames = {"portfolio_id", "spin_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WheelSpin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "spin_date", nullable = false)
    private LocalDate spinDate;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal gain;
}
