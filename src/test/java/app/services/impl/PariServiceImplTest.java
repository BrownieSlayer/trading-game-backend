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

import app.dto.market.PriceQuote;
import app.dto.minigames.BetDto;
import app.enums.BetDirection;
import app.enums.BetStatus;
import app.exceptions.InsufficientFundsException;
import app.exceptions.InvalidQuantityException;
import app.models.Bet;
import app.models.Portfolio;
import app.models.User;
import app.repositories.BetRepository;
import app.repositories.PortfolioRepository;
import app.services.market.MarketPriceCacheService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Vérifie le pari du jour porté depuis {@code minigames/pari.py} : mise réservée, gain à 0.8x, résolution vs cours de référence. */
@ExtendWith(MockitoExtension.class)
class PariServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 17);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private BetRepository betRepository;
    @Mock
    private MarketPriceCacheService marketPriceCacheService;

    private PariServiceImpl service;
    private User user;
    private Portfolio portfolio;

    private void givenPortfolio(BigDecimal cash) {
        service = new PariServiceImpl(portfolioRepository, betRepository, marketPriceCacheService, CLOCK);

        user = new User();
        user.setId(1L);

        portfolio = new Portfolio();
        portfolio.setId(10L);
        portfolio.setUser(user);
        portfolio.setCash(cash);
    }

    @Test
    void placingABetReservesTheStakeImmediately() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(betRepository.findByPortfolioIdAndStatus(10L, BetStatus.ACTIVE)).thenReturn(Optional.empty());
        when(marketPriceCacheService.getQuote("AAPL")).thenReturn(Optional.of(new PriceQuote("AAPL", 100.0, 0.0)));
        when(betRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BetDto bet = service.placeBet(user, "AAPL", BetDirection.UP, new BigDecimal("50"));

        assertThat(bet.status()).isEqualTo(BetStatus.ACTIVE);
        assertThat(bet.referencePrice()).isEqualByComparingTo("100.0000");
        assertThat(portfolio.getCash()).isEqualByComparingTo("9950.00");
    }

    @Test
    void nullStakeFallsBackToTheDefaultStake() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(betRepository.findByPortfolioIdAndStatus(10L, BetStatus.ACTIVE)).thenReturn(Optional.empty());
        when(marketPriceCacheService.getQuote("AAPL")).thenReturn(Optional.of(new PriceQuote("AAPL", 100.0, 0.0)));
        when(betRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BetDto bet = service.placeBet(user, "AAPL", BetDirection.UP, null);

        assertThat(bet.stake()).isEqualByComparingTo("50.00"); // MinigameConstants.Pari.DEFAULT_STAKE
    }

    @Test
    void cannotPlaceASecondBetWhileOneIsActive() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(betRepository.findByPortfolioIdAndStatus(10L, BetStatus.ACTIVE)).thenReturn(Optional.of(new Bet()));

        assertThatThrownBy(() -> service.placeBet(user, "AAPL", BetDirection.UP, new BigDecimal("50")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotPlaceABetWithInsufficientFunds() {
        givenPortfolio(new BigDecimal("10.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(betRepository.findByPortfolioIdAndStatus(10L, BetStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.placeBet(user, "AAPL", BetDirection.UP, new BigDecimal("50")))
            .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void cannotPlaceABetOnAnUnknownTicker() {
        givenPortfolio(new BigDecimal("10000.00"));

        assertThatThrownBy(() -> service.placeBet(user, "NOT-A-TICKER", BetDirection.UP, new BigDecimal("50")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotPlaceABetWithANonPositiveStake() {
        givenPortfolio(new BigDecimal("10000.00"));

        assertThatThrownBy(() -> service.placeBet(user, "AAPL", BetDirection.UP, BigDecimal.ZERO))
            .isInstanceOf(InvalidQuantityException.class);
    }

    private Bet activeBet(BetDirection direction, BigDecimal stake, BigDecimal referencePrice) {
        Bet bet = new Bet();
        bet.setPortfolio(portfolio);
        bet.setTicker("AAPL");
        bet.setDirection(direction);
        bet.setStake(stake);
        bet.setReferencePrice(referencePrice);
        bet.setStatus(BetStatus.ACTIVE);
        return bet;
    }

    @Test
    void resolvingAWinningBetCreditsStakePlusGain() {
        givenPortfolio(new BigDecimal("9950.00")); // mise de 50 déjà déduite
        Bet bet = activeBet(BetDirection.UP, new BigDecimal("50.00"), new BigDecimal("100.0000"));
        when(betRepository.findByPortfolioIdAndStatus(10L, BetStatus.ACTIVE)).thenReturn(Optional.of(bet));
        when(marketPriceCacheService.getQuote("AAPL")).thenReturn(Optional.of(new PriceQuote("AAPL", 110.0, 0.0)));

        service.resolveActiveBet(portfolio, TODAY);

        assertThat(bet.getStatus()).isEqualTo(BetStatus.WON);
        // gain = 50 * 0.8 = 40 ; cash = 9950 + 50 + 40 = 10040
        assertThat(bet.getGain()).isEqualByComparingTo("40.00");
        assertThat(portfolio.getCash()).isEqualByComparingTo("10040.00");
    }

    @Test
    void resolvingALosingBetNeverCreditsCashBack() {
        givenPortfolio(new BigDecimal("9950.00")); // mise de 50 déjà déduite, reste perdue
        Bet bet = activeBet(BetDirection.UP, new BigDecimal("50.00"), new BigDecimal("100.0000"));
        when(betRepository.findByPortfolioIdAndStatus(10L, BetStatus.ACTIVE)).thenReturn(Optional.of(bet));
        when(marketPriceCacheService.getQuote("AAPL")).thenReturn(Optional.of(new PriceQuote("AAPL", 90.0, 0.0)));

        service.resolveActiveBet(portfolio, TODAY);

        assertThat(bet.getStatus()).isEqualTo(BetStatus.LOST);
        assertThat(bet.getGain()).isEqualByComparingTo("-50.00");
        assertThat(portfolio.getCash()).isEqualByComparingTo("9950.00"); // inchangé, la mise reste perdue
    }

    @Test
    void resolutionIsSkippedWhenThePriceIsStillUnavailable() {
        givenPortfolio(new BigDecimal("9950.00"));
        Bet bet = activeBet(BetDirection.UP, new BigDecimal("50.00"), new BigDecimal("100.0000"));
        when(betRepository.findByPortfolioIdAndStatus(10L, BetStatus.ACTIVE)).thenReturn(Optional.of(bet));
        when(marketPriceCacheService.getQuote("AAPL")).thenReturn(Optional.empty());

        service.resolveActiveBet(portfolio, TODAY);

        assertThat(bet.getStatus()).isEqualTo(BetStatus.ACTIVE);
    }

    @Test
    void resolutionIsANoOpWhenThereIsNoActiveBet() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(betRepository.findByPortfolioIdAndStatus(10L, BetStatus.ACTIVE)).thenReturn(Optional.empty());

        service.resolveActiveBet(portfolio, TODAY);

        assertThat(portfolio.getCash()).isEqualByComparingTo("10000.00");
    }
}
