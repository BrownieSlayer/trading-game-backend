package app.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import app.dto.minigames.BetDto;
import app.enums.BetDirection;
import app.models.Portfolio;
import app.models.User;

/**
 * Port de {@code minigames/pari.py} : un seul pari actif à la fois, mise
 * réservée immédiatement, résolution automatique à la prochaine session
 * (voir {@link DailyCycleService}).
 */
public interface PariService {

    /** Place un nouveau pari. {@code stake} peut être null (mise par défaut du jeu). */
    BetDto placeBet(User user, String ticker, BetDirection direction, BigDecimal stake);

    Optional<BetDto> getActiveBet(User user);

    /**
     * Résout le pari actif du portefeuille s'il y en a un et que le cours du
     * ticker est disponible. Ne fait rien sinon (on réessaiera à la
     * prochaine résolution du "nouveau jour"). Appelée par
     * {@link DailyCycleService}, jamais directement par un contrôleur.
     */
    void resolveActiveBet(Portfolio portfolio, LocalDate today);
}
