package app.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import app.enums.ReflexSessionStatus;
import app.models.converter.BigDecimalListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une partie de trading rapide (réflexe) — port de
 * {@code minigames/trading_rapide.py}. La marche aléatoire des 25 ticks est
 * générée et figée côté serveur dès {@code start()} ; les actions du joueur
 * sont toujours revalidées contre le tick réellement écoulé
 * ({@code startedAt} vs horloge serveur), jamais contre un prix envoyé par
 * le client.
 */
@Entity
@Table(name = "reflex_trading_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReflexTradingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    /** Jour civil auquel cette partie compte, pour la limite de 3 essais/jour. */
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Convert(converter = BigDecimalListConverter.class)
    @Column(nullable = false, length = 1000)
    private List<BigDecimal> prices;

    @Column(name = "buy_tick_index")
    private Integer buyTickIndex;

    @Column(name = "sell_tick_index")
    private Integer sellTickIndex;

    @Column(precision = 14, scale = 2)
    private BigDecimal gain;

    @Column(nullable = false)
    private boolean abandoned = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReflexSessionStatus status = ReflexSessionStatus.OPEN;
}
