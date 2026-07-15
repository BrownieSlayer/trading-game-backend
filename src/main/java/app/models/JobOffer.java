package app.models;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une offre d'emploi proposée — transitoire, remplacée en bloc à chaque recherche, supprimée dès qu'une est acceptée. */
@Entity
@Table(name = "job_offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 100)
    private String title;

    /** "nul", "median" ou "fou" — voir MinigameConstants.Emploi.TIERS. */
    @Column(nullable = false, length = 10)
    private String tier;

    @Column(name = "duration_hours", nullable = false)
    private int durationHours;

    @Column(name = "hourly_wage", nullable = false, precision = 14, scale = 2)
    private BigDecimal hourlyWage;

    @Column(name = "total_wage", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalWage;
}
