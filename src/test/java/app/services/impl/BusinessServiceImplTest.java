package app.services.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.dto.minigames.BusinessDto;
import app.exceptions.InsufficientFundsException;
import app.exceptions.InvalidQuantityException;
import app.models.BusinessState;
import app.models.Portfolio;
import app.models.User;
import app.repositories.BusinessStateRepository;
import app.repositories.PortfolioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Vérifie le Business (revenu passif) porté depuis {@code minigames/business.py} :
 * rendement décroissant, aléa ±10% par heure, plafond hors-ligne, coûts
 * exponentiels des améliorations.
 */
@ExtendWith(MockitoExtension.class)
class BusinessServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("UTC"));

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private BusinessStateRepository businessStateRepository;

    private User user;
    private Portfolio portfolio;

    private BusinessServiceImpl service() {
        return new BusinessServiceImpl(portfolioRepository, businessStateRepository, CLOCK, new Random(7));
    }

    private void givenPortfolio(BigDecimal cash) {
        user = new User();
        user.setId(1L);

        portfolio = new Portfolio();
        portfolio.setId(10L);
        portfolio.setUser(user);
        portfolio.setCash(cash);
    }

    private BusinessState stateWith(BigDecimal investment, Instant lastHarvestAt) {
        BusinessState state = new BusinessState();
        state.setId(20L);
        state.setPortfolio(portfolio);
        state.setInvestment(investment);
        state.setLastHarvestAt(lastHarvestAt);
        state.setTotalHarvested(BigDecimal.ZERO.setScale(2));
        return state;
    }

    @Test
    void investingReservesCashAndSettlesPendingHarvestFirst() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        BusinessState state = stateWith(BigDecimal.ZERO.setScale(2), null);
        when(businessStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(state));

        BusinessDto dto = service().invest(user, new BigDecimal("1000"));

        assertThat(dto.investment()).isEqualByComparingTo("1000.00");
        assertThat(portfolio.getCash()).isEqualByComparingTo("9000.00");
        assertThat(state.getLastHarvestAt()).isEqualTo(NOW); // première récolte simplement amorcée
    }

    @Test
    void investingRejectsANonPositiveAmount() {
        givenPortfolio(new BigDecimal("10000.00"));

        assertThatThrownBy(() -> service().invest(user, BigDecimal.ZERO)).isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void investingRejectsInsufficientFunds() {
        givenPortfolio(new BigDecimal("100.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(businessStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(stateWith(BigDecimal.ZERO.setScale(2), null)));

        assertThatThrownBy(() -> service().invest(user, new BigDecimal("1000")))
            .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void investingRejectsExceedingTheInvestmentCap() {
        givenPortfolio(new BigDecimal("100000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(businessStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(stateWith(BigDecimal.ZERO.setScale(2), null)));

        // plafond de base = 20000 €
        assertThatThrownBy(() -> service().invest(user, new BigDecimal("25000")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void withdrawingReturnsCashToThePortfolio() {
        givenPortfolio(new BigDecimal("5000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        BusinessState state = stateWith(new BigDecimal("1000.00"), NOW);
        when(businessStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(state));

        BusinessDto dto = service().withdraw(user, new BigDecimal("400"));

        assertThat(dto.investment()).isEqualByComparingTo("600.00");
        assertThat(portfolio.getCash()).isEqualByComparingTo("5400.00");
    }

    @Test
    void withdrawingMoreThanInvestedIsRejected() {
        givenPortfolio(new BigDecimal("5000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(businessStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(stateWith(new BigDecimal("100.00"), NOW)));

        assertThatThrownBy(() -> service().withdraw(user, new BigDecimal("200")))
            .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void upgradingIncrementsTheLevelAndDeductsTheCost() {
        givenPortfolio(new BigDecimal("1000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        BusinessState state = stateWith(BigDecimal.ZERO.setScale(2), NOW);
        when(businessStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(state));

        // capacite niveau 0 -> coût de base 500 €
        BusinessDto dto = service().upgrade(user, "capacite");

        assertThat(dto.upgrades().get("capacite").level()).isEqualTo(1);
        assertThat(portfolio.getCash()).isEqualByComparingTo("500.00");
    }

    @Test
    void upgradingWithInsufficientFundsIsRejected() {
        givenPortfolio(new BigDecimal("10.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(businessStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(stateWith(BigDecimal.ZERO.setScale(2), NOW)));

        assertThatThrownBy(() -> service().upgrade(user, "capacite")).isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void upgradingAnUnknownTypeIsRejected() {
        givenPortfolio(new BigDecimal("10000.00"));

        assertThatThrownBy(() -> service().upgrade(user, "not-a-real-upgrade")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void harvestingCreditsNothingWhenNoInvestment() {
        givenPortfolio(new BigDecimal("1000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(businessStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(stateWith(BigDecimal.ZERO.setScale(2), NOW.minusSeconds(7200))));
        when(businessStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().getStatus(user);

        assertThat(portfolio.getCash()).isEqualByComparingTo("1000.00");
    }

    @Test
    void harvestingCreditsWithinTheExpectedNoiseBoundsForTwoElapsedHours() {
        givenPortfolio(new BigDecimal("1000.00"));
        // investissement 10000, niveau rendement 0 -> taux 0.5%/h, coefficient = 1/(1+10000/5000) = 1/3
        // base horaire = 10000 * 0.005 * (1/3) = 16.6667 € ; aléa ±10% ; 2 heures écoulées (< plafond 24h)
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        BusinessState state = stateWith(new BigDecimal("10000.00"), NOW.minusSeconds(7200));
        when(businessStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(state));
        when(businessStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().getStatus(user);

        BigDecimal credited = portfolio.getCash().subtract(new BigDecimal("1000.00"));
        // bornes : 2 * 16.6667 * [0.9, 1.1] = [30.00, 36.67]
        assertThat(credited).isGreaterThanOrEqualTo(new BigDecimal("29.99"));
        assertThat(credited).isLessThanOrEqualTo(new BigDecimal("36.68"));
        assertThat(state.getLastHarvestAt()).isEqualTo(NOW.minusSeconds(7200).plusSeconds(2 * 3600));
    }

    @Test
    void harvestingBeyondTheOfflineCapResynchronisesToNowAndLosesTheSurplus() {
        givenPortfolio(new BigDecimal("1000.00"));
        // 30h écoulées, plafond hors-ligne de base = 24h (niveau stockage 0) : seules 24h sont créditées.
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        BusinessState state = stateWith(new BigDecimal("10000.00"), NOW.minusSeconds(30 * 3600));
        when(businessStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(state));
        when(businessStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().getStatus(user);

        BigDecimal credited = portfolio.getCash().subtract(new BigDecimal("1000.00"));
        // bornes pour 24h : 24 * 16.6667 * [0.9, 1.1] = [360.00, 440.00]
        assertThat(credited).isGreaterThanOrEqualTo(new BigDecimal("359.99"));
        assertThat(credited).isLessThanOrEqualTo(new BigDecimal("440.01"));
        assertThat(state.getLastHarvestAt()).isEqualTo(NOW); // resynchronisé, pas de rattrapage infini
    }
}
