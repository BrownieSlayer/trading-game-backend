package app.models;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** État du mini-jeu Business (revenu passif) d'un portefeuille — port de {@code minigames/business.py}. */
@Entity
@Table(name = "business_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false, unique = true)
    private Portfolio portfolio;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal investment = BigDecimal.ZERO.setScale(2);

    @Column(name = "last_harvest_at")
    private Instant lastHarvestAt;

    @Column(name = "capacity_level", nullable = false)
    private int capacityLevel = 0;

    @Column(name = "yield_level", nullable = false)
    private int yieldLevel = 0;

    @Column(name = "storage_level", nullable = false)
    private int storageLevel = 0;

    @Column(name = "total_harvested", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalHarvested = BigDecimal.ZERO.setScale(2);
}
