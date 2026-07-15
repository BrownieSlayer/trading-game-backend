package app.models;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * État du mini-jeu Emploi d'un portefeuille — port de
 * {@code minigames/emploi.py}. Le poste actif (au plus un à la fois) vit
 * directement sur cette ligne plutôt que dans une table dédiée, comme
 * l'investissement du Business — les offres proposées (0 à N, transitoires,
 * remplacées à chaque recherche) sont dans {@link JobOffer}.
 */
@Entity
@Table(name = "employment_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false, unique = true)
    private Portfolio portfolio;

    @Column(name = "manual_level", nullable = false)
    private int manualLevel = 0;

    @Column(name = "intellect_level", nullable = false)
    private int intellectLevel = 0;

    @Column(name = "computer_level", nullable = false)
    private int computerLevel = 0;

    @Column(name = "total_earned", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalEarned = BigDecimal.ZERO.setScale(2);

    @Column(name = "active_job_title", length = 100)
    private String activeJobTitle;

    /** "nul", "median" ou "fou" — voir MinigameConstants.Emploi.TIERS. */
    @Column(name = "active_job_tier", length = 10)
    private String activeJobTier;

    @Column(name = "active_job_duration_hours")
    private Integer activeJobDurationHours;

    @Column(name = "active_job_started_at")
    private Instant activeJobStartedAt;

    @Column(name = "active_job_ends_at")
    private Instant activeJobEndsAt;

    @Column(name = "active_job_hourly_wage", precision = 14, scale = 2)
    private BigDecimal activeJobHourlyWage;

    @Column(name = "active_job_total_wage", precision = 14, scale = 2)
    private BigDecimal activeJobTotalWage;
}
