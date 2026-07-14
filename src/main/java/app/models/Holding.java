package app.models;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une ligne de position détenue dans un portefeuille (quantité + prix d'achat moyen pondéré). */
@Entity
@Table(
    name = "holdings",
    uniqueConstraints = @UniqueConstraint(name = "uq_holdings_portfolio_ticker", columnNames = {"portfolio_id", "ticker"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "average_buy_price", nullable = false, precision = 14, scale = 4)
    private BigDecimal averageBuyPrice;
}
