package app.services.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.models.Portfolio;
import app.models.User;
import app.repositories.PortfolioRepository;
import app.repositories.PortfolioValueSnapshotRepository;
import app.services.PariService;
import app.services.PortfolioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Vérifie la résolution du "nouveau jour" portée depuis
 * {@code core/portfolio.py:is_new_day}/{@code update_streak} : streak,
 * règle vendredi -> lundi, idempotence intra-journée, snapshot de valeur.
 *
 * Dates de référence fixes (au lieu de LocalDate.now()) : 2026-07-17 est un
 * vendredi, 2026-07-20 le lundi suivant.
 */
@ExtendWith(MockitoExtension.class)
class DailyCycleServiceImplTest {

    private static final LocalDate FRIDAY = LocalDate.of(2026, 7, 17);
    private static final LocalDate MONDAY = LocalDate.of(2026, 7, 20);
    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private PortfolioValueSnapshotRepository portfolioValueSnapshotRepository;
    @Mock
    private PortfolioService portfolioService;
    @Mock
    private PariService pariService;

    private User user;
    private Portfolio portfolio;

    private DailyCycleServiceImpl serviceAt(LocalDate today) {
        Clock clock = Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
        return new DailyCycleServiceImpl(portfolioRepository, portfolioValueSnapshotRepository, portfolioService, pariService, clock);
    }

    private void givenPortfolio(LocalDate lastSessionDate, int streak) {
        user = new User();
        user.setId(1L);

        portfolio = new Portfolio();
        portfolio.setId(10L);
        portfolio.setUser(user);
        portfolio.setLastSessionDate(lastSessionDate);
        portfolio.setStreak(streak);

        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
    }

    @Test
    void firstEverSessionStartsTheStreakAtOne() {
        givenPortfolio(null, 0);
        when(portfolioService.getTotalValue(portfolio)).thenReturn(new BigDecimal("10000.00"));

        serviceAt(FRIDAY).resolveNewDay(user);

        assertThat(portfolio.getStreak()).isEqualTo(1);
        assertThat(portfolio.getLastSessionDate()).isEqualTo(FRIDAY);
    }

    @Test
    void consecutiveDayIncrementsTheStreak() {
        givenPortfolio(FRIDAY.minusDays(1), 4);
        when(portfolioService.getTotalValue(portfolio)).thenReturn(new BigDecimal("10000.00"));

        serviceAt(FRIDAY).resolveNewDay(user);

        assertThat(portfolio.getStreak()).isEqualTo(5);
    }

    @Test
    void fridayToMondayDoesNotBreakTheStreak() {
        givenPortfolio(FRIDAY, 4);
        when(portfolioService.getTotalValue(portfolio)).thenReturn(new BigDecimal("10000.00"));

        serviceAt(MONDAY).resolveNewDay(user);

        assertThat(portfolio.getStreak()).isEqualTo(5);
    }

    @Test
    void aGapLargerThanAWeekendResetsTheStreak() {
        // Jeudi -> lundi suivant (4 jours, dernière session pas un vendredi) : rupture.
        givenPortfolio(MONDAY.minusDays(4), 7);
        when(portfolioService.getTotalValue(portfolio)).thenReturn(new BigDecimal("10000.00"));

        serviceAt(MONDAY).resolveNewDay(user);

        assertThat(portfolio.getStreak()).isEqualTo(1);
    }

    @Test
    void repeatedCallsOnTheSameDayAreNoOps() {
        givenPortfolio(FRIDAY, 3);

        serviceAt(FRIDAY).resolveNewDay(user);

        assertThat(portfolio.getStreak()).isEqualTo(3);
        verify(portfolioRepository, never()).save(any());
        verify(portfolioService, never()).getTotalValue(any());
    }

    @Test
    void recordsAValueSnapshotOnceADay() {
        givenPortfolio(FRIDAY.minusDays(1), 4);
        when(portfolioService.getTotalValue(portfolio)).thenReturn(new BigDecimal("12345.67"));

        serviceAt(FRIDAY).resolveNewDay(user);

        verify(portfolioValueSnapshotRepository).save(any());
    }

    @Test
    void doesNotDuplicateAnExistingSnapshotForTheSameDay() {
        givenPortfolio(FRIDAY.minusDays(1), 4);
        when(portfolioValueSnapshotRepository.existsByPortfolioIdAndSnapshotDate(10L, FRIDAY)).thenReturn(true);

        serviceAt(FRIDAY).resolveNewDay(user);

        verify(portfolioService, never()).getTotalValue(any());
        verify(portfolioValueSnapshotRepository, never()).save(any());
    }
}
