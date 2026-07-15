package app.services.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.configuration.game.MinigameConstants;
import app.configuration.game.WheelResult;
import app.dto.minigames.WheelSpinDto;
import app.models.Portfolio;
import app.models.User;
import app.models.WheelSpin;
import app.repositories.PortfolioRepository;
import app.repositories.WheelSpinRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Vérifie la roue de la fortune portée depuis {@code minigames/roue.py} :
 * tirage pondéré (poids 40/35/15/8/2, voir MinigameConstants.Roue), verrou
 * quotidien, gain appliqué aux liquidités.
 */
@ExtendWith(MockitoExtension.class)
class RoueServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 17);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private WheelSpinRepository wheelSpinRepository;
    @Mock
    private Random random;

    private User user;
    private Portfolio portfolio;

    private void givenPortfolio() {
        user = new User();
        user.setId(1L);

        portfolio = new Portfolio();
        portfolio.setId(10L);
        portfolio.setUser(user);
        portfolio.setCash(new BigDecimal("10000.00"));

        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
    }

    // --- Bornes du tirage pondéré (poids 40/35/15/8/2, total 100) ---------

    @Test
    void pickResultCoversEachWeightedBucketByItsBoundaries() {
        List<WheelResult> results = MinigameConstants.Roue.RESULTS;

        assertThat(RoueServiceImpl.pickResult(results, 0).label()).isEqualTo("Rien ce coup-ci");
        assertThat(RoueServiceImpl.pickResult(results, 39).label()).isEqualTo("Rien ce coup-ci");
        assertThat(RoueServiceImpl.pickResult(results, 40).label()).isEqualTo("Petit bonus");
        assertThat(RoueServiceImpl.pickResult(results, 74).label()).isEqualTo("Petit bonus");
        assertThat(RoueServiceImpl.pickResult(results, 75).label()).isEqualTo("Bon bonus");
        assertThat(RoueServiceImpl.pickResult(results, 89).label()).isEqualTo("Bon bonus");
        assertThat(RoueServiceImpl.pickResult(results, 90).label()).isEqualTo("Gros lot");
        assertThat(RoueServiceImpl.pickResult(results, 97).label()).isEqualTo("Gros lot");
        assertThat(RoueServiceImpl.pickResult(results, 98).label()).isEqualTo("JACKPOT");
        assertThat(RoueServiceImpl.pickResult(results, 99).label()).isEqualTo("JACKPOT");
    }

    // --- Comportement du service --------------------------------------

    @Test
    void spinningCreditsTheDrawnGainToCash() {
        givenPortfolio();
        when(wheelSpinRepository.existsByPortfolioIdAndSpinDate(10L, TODAY)).thenReturn(false);
        when(random.nextInt(100)).thenReturn(98); // JACKPOT, +500
        when(wheelSpinRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoueServiceImpl service = new RoueServiceImpl(portfolioRepository, wheelSpinRepository, CLOCK, random);
        WheelSpinDto result = service.spin(user);

        assertThat(result.label()).isEqualTo("JACKPOT");
        assertThat(result.gain()).isEqualByComparingTo("500.00");
        assertThat(portfolio.getCash()).isEqualByComparingTo("10500.00");
    }

    @Test
    void cannotSpinTwiceOnTheSameDay() {
        givenPortfolio();
        when(wheelSpinRepository.existsByPortfolioIdAndSpinDate(10L, TODAY)).thenReturn(true);

        RoueServiceImpl service = new RoueServiceImpl(portfolioRepository, wheelSpinRepository, CLOCK, random);

        assertThatThrownBy(() -> service.spin(user)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getTodayResultReturnsEmptyWhenNotYetSpun() {
        givenPortfolio();
        when(wheelSpinRepository.findByPortfolioIdAndSpinDate(10L, TODAY)).thenReturn(Optional.empty());

        RoueServiceImpl service = new RoueServiceImpl(portfolioRepository, wheelSpinRepository, CLOCK, random);

        assertThat(service.getTodayResult(user)).isEmpty();
    }

    @Test
    void getTodayResultReturnsThePreviousSpinWhenAlreadyPlayed() {
        givenPortfolio();
        WheelSpin spin = new WheelSpin();
        spin.setPortfolio(portfolio);
        spin.setSpinDate(TODAY);
        spin.setLabel("Petit bonus");
        spin.setGain(new BigDecimal("10.00"));
        when(wheelSpinRepository.findByPortfolioIdAndSpinDate(10L, TODAY)).thenReturn(Optional.of(spin));

        RoueServiceImpl service = new RoueServiceImpl(portfolioRepository, wheelSpinRepository, CLOCK, random);

        assertThat(service.getTodayResult(user)).isPresent();
        assertThat(service.getTodayResult(user).get().label()).isEqualTo("Petit bonus");
    }
}
