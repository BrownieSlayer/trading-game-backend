package app.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import app.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un achat ou une vente exécuté sur un portefeuille — journal immuable, jamais modifié après création. */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

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
    private TransactionType type;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal price;

    /** Plus-value réalisée (uniquement renseignée pour une vente). */
    @Column(name = "capital_gain", precision = 14, scale = 2)
    private BigDecimal capitalGain;

    /** Impôt PFU prélevé (uniquement renseigné pour une vente, 0 en cas de moins-value). */
    @Column(precision = 14, scale = 2)
    private BigDecimal tax;

    @CreationTimestamp
    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt;
}
