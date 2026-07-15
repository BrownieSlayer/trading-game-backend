package app.services.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.dto.minigames.TradingRapideStatusDto;
import app.enums.ReflexAction;
import app.enums.ReflexSessionStatus;
import app.models.Portfolio;
import app.models.ReflexTradingSession;
import app.models.User;
import app.repositories.PortfolioRepository;
import app.repositories.ReflexTradingSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Vérifie le trading rapide porté depuis {@code minigames/trading_rapide.py} :
 * marche aléatoire à 25 ticks, gain sur mise notionnelle, revalidation
 * serveur du tick contre l'horloge (jamais un prix fourni par le client),
 * limite de 3 essais/jour, résolution automatique d'une partie expirée.
 */
@ExtendWith(MockitoExtension.class)
class TradingRapideServiceImplTest {

    private static final Instant NOW = LocalDate.of(2026, 7, 17).atStartOfDay(ZoneId.systemDefault()).toInstant();
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.systemDefault());
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 17);

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private ReflexTradingSessionRepository sessionRepository;

    private User user;
    private Portfolio portfolio;

    private TradingRapideServiceImpl service() {
        return new TradingRapideServiceImpl(portfolioRepository, sessionRepository, CLOCK, new Random(42));
    }

    private void givenPortfolio(BigDecimal cash) {
        user = new User();
        user.setId(1L);

        portfolio = new Portfolio();
        portfolio.setId(10L);
        portfolio.setUser(user);
        portfolio.setCash(cash);

        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
    }

    private List<BigDecimal> fixedPrices() {
        // 25 ticks, un par index, valeurs choisies pour rendre les tests lisibles.
        List<BigDecimal> prices = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) {
            prices.add(BigDecimal.valueOf(100.0 + i).setScale(4));
        }
        return prices;
    }

    private ReflexTradingSession openSession(Instant startedAt) {
        ReflexTradingSession session = new ReflexTradingSession();
        session.setId(99L);
        session.setPortfolio(portfolio);
        session.setSessionDate(TODAY);
        session.setStartedAt(startedAt);
        session.setPrices(fixedPrices());
        session.setStatus(ReflexSessionStatus.OPEN);
        return session;
    }

    // --- Fonctions pures ------------------------------------------------

    @Test
    void computeGainOnAGainingRoundTrip() {
        BigDecimal gain = TradingRapideServiceImpl.computeGain(new BigDecimal("100.0000"), new BigDecimal("110.0000"));
        // (10/100) * 200 (mise notionnelle) = 20.00
        assertThat(gain).isEqualByComparingTo("20.00");
    }

    @Test
    void computeGainOnALosingRoundTrip() {
        BigDecimal gain = TradingRapideServiceImpl.computeGain(new BigDecimal("100.0000"), new BigDecimal("90.0000"));
        assertThat(gain).isEqualByComparingTo("-20.00");
    }

    @Test
    void currentTickIndexTracksElapsedTimeInIntervalsOf750Ms() {
        Instant startedAt = NOW;
        assertThat(TradingRapideServiceImpl.currentTickIndex(startedAt, startedAt)).isEqualTo(0);
        assertThat(TradingRapideServiceImpl.currentTickIndex(startedAt, startedAt.plusMillis(749))).isEqualTo(0);
        assertThat(TradingRapideServiceImpl.currentTickIndex(startedAt, startedAt.plusMillis(750))).isEqualTo(1);
        assertThat(TradingRapideServiceImpl.currentTickIndex(startedAt, startedAt.plusMillis(1500))).isEqualTo(2);
    }

    @Test
    void currentTickIndexIsClampedToTheLastTickPastTheSessionDuration() {
        Instant startedAt = NOW;
        assertThat(TradingRapideServiceImpl.currentTickIndex(startedAt, startedAt.plusMillis(18_750))).isEqualTo(24);
        assertThat(TradingRapideServiceImpl.currentTickIndex(startedAt, startedAt.plusSeconds(600))).isEqualTo(24);
    }

    // --- start() ----------------------------------------------------------

    @Test
    void startingCreatesAnOpenSessionWithTwentyFiveTicks() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(sessionRepository.findByPortfolioIdAndStatus(10L, ReflexSessionStatus.OPEN)).thenReturn(Optional.empty());
        when(sessionRepository.countByPortfolioIdAndSessionDateAndStatus(10L, TODAY, ReflexSessionStatus.RESOLVED)).thenReturn(0L);
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TradingRapideStatusDto status = service().start(user);

        assertThat(status.session()).isNotNull();
        assertThat(status.session().prices()).hasSize(25);
        assertThat(status.session().status()).isEqualTo(ReflexSessionStatus.OPEN);
        assertThat(status.attemptsRemainingToday()).isEqualTo(3);
    }

    @Test
    void cannotStartASecondSessionWhileOneIsOpen() {
        givenPortfolio(new BigDecimal("10000.00"));
        ReflexTradingSession stillRunning = openSession(NOW); // elapsed = 0, pas expirée
        when(sessionRepository.findByPortfolioIdAndStatus(10L, ReflexSessionStatus.OPEN)).thenReturn(Optional.of(stillRunning));

        assertThatThrownBy(() -> service().start(user)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotStartMoreThanThreeSessionsPerDay() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(sessionRepository.findByPortfolioIdAndStatus(10L, ReflexSessionStatus.OPEN)).thenReturn(Optional.empty());
        when(sessionRepository.countByPortfolioIdAndSessionDateAndStatus(10L, TODAY, ReflexSessionStatus.RESOLVED)).thenReturn(3L);

        assertThatThrownBy(() -> service().start(user)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startingAutoResolvesAStaleOpenPositionAsAnAutomaticSaleAtTheLastTick() {
        givenPortfolio(new BigDecimal("10000.00"));
        ReflexTradingSession stale = openSession(NOW.minusSeconds(60)); // très au-delà de la durée de partie
        stale.setBuyTickIndex(0); // position ouverte au tick 0 (prix 100.00)
        when(sessionRepository.findByPortfolioIdAndStatus(10L, ReflexSessionStatus.OPEN))
            .thenReturn(Optional.of(stale))
            .thenReturn(Optional.empty()); // plus rien d'ouvert une fois auto-résolu
        when(sessionRepository.countByPortfolioIdAndSessionDateAndStatus(10L, TODAY, ReflexSessionStatus.RESOLVED)).thenReturn(0L);
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().start(user);

        // vente auto au dernier tick (index 24, prix 124.00) : gain = (24/100)*200 = 48.00
        assertThat(stale.getStatus()).isEqualTo(ReflexSessionStatus.RESOLVED);
        assertThat(stale.getGain()).isEqualByComparingTo("48.00");
        assertThat(portfolio.getCash()).isEqualByComparingTo("10048.00");
    }

    // --- act() ----------------------------------------------------------

    @Test
    void buyingOpensAPositionAtTheCurrentTick() {
        givenPortfolio(new BigDecimal("10000.00"));
        ReflexTradingSession session = openSession(NOW.minusMillis(1500)); // tick courant = 2
        when(sessionRepository.findByPortfolioIdAndStatus(10L, ReflexSessionStatus.OPEN)).thenReturn(Optional.of(session));
        when(sessionRepository.countByPortfolioIdAndSessionDateAndStatus(10L, TODAY, ReflexSessionStatus.RESOLVED)).thenReturn(0L);
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().act(user, ReflexAction.BUY);

        assertThat(session.getBuyTickIndex()).isEqualTo(2);
        assertThat(session.getStatus()).isEqualTo(ReflexSessionStatus.OPEN);
    }

    @Test
    void cannotBuyTwiceInTheSameRound() {
        givenPortfolio(new BigDecimal("10000.00"));
        ReflexTradingSession session = openSession(NOW);
        session.setBuyTickIndex(0);
        when(sessionRepository.findByPortfolioIdAndStatus(10L, ReflexSessionStatus.OPEN)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service().act(user, ReflexAction.BUY)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sellingClosesThePositionAndCreditsTheGainToCash() {
        givenPortfolio(new BigDecimal("10000.00"));
        ReflexTradingSession session = openSession(NOW.minusMillis(2250)); // tick courant = 3, prix 103.00
        session.setBuyTickIndex(0); // acheté à 100.00
        when(sessionRepository.findByPortfolioIdAndStatus(10L, ReflexSessionStatus.OPEN)).thenReturn(Optional.of(session));
        when(sessionRepository.countByPortfolioIdAndSessionDateAndStatus(10L, TODAY, ReflexSessionStatus.RESOLVED)).thenReturn(0L);
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TradingRapideStatusDto status = service().act(user, ReflexAction.SELL);

        // (103-100)/100 * 200 = 6.00
        assertThat(status.session().gain()).isEqualByComparingTo("6.00");
        assertThat(status.session().status()).isEqualTo(ReflexSessionStatus.RESOLVED);
        assertThat(portfolio.getCash()).isEqualByComparingTo("10006.00");
    }

    @Test
    void cannotSellWithoutAnOpenPosition() {
        givenPortfolio(new BigDecimal("10000.00"));
        ReflexTradingSession session = openSession(NOW);
        when(sessionRepository.findByPortfolioIdAndStatus(10L, ReflexSessionStatus.OPEN)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service().act(user, ReflexAction.SELL)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void abandoningNeverChangesCashEvenWithAnOpenPosition() {
        givenPortfolio(new BigDecimal("10000.00"));
        ReflexTradingSession session = openSession(NOW.minusMillis(3000));
        session.setBuyTickIndex(0);
        when(sessionRepository.findByPortfolioIdAndStatus(10L, ReflexSessionStatus.OPEN)).thenReturn(Optional.of(session));
        when(sessionRepository.countByPortfolioIdAndSessionDateAndStatus(10L, TODAY, ReflexSessionStatus.RESOLVED)).thenReturn(0L);
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TradingRapideStatusDto status = service().act(user, ReflexAction.ABANDON);

        assertThat(status.session().abandoned()).isTrue();
        assertThat(status.session().gain()).isEqualByComparingTo("0.00");
        assertThat(portfolio.getCash()).isEqualByComparingTo("10000.00");
    }

    @Test
    void actingWithoutAnOpenSessionIsRejected() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(sessionRepository.findByPortfolioIdAndStatus(10L, ReflexSessionStatus.OPEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().act(user, ReflexAction.BUY)).isInstanceOf(IllegalStateException.class);
    }

    // --- getStatus() ------------------------------------------------------

    @Test
    void statusReportsNoSessionWhenNoneIsOpen() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(sessionRepository.findByPortfolioIdAndStatus(10L, ReflexSessionStatus.OPEN)).thenReturn(Optional.empty());
        when(sessionRepository.countByPortfolioIdAndSessionDateAndStatus(10L, TODAY, ReflexSessionStatus.RESOLVED)).thenReturn(1L);

        TradingRapideStatusDto status = service().getStatus(user);

        assertThat(status.session()).isNull();
        assertThat(status.attemptsRemainingToday()).isEqualTo(2);
    }
}
