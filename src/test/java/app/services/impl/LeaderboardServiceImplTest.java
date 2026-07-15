package app.services.impl;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.dto.leaderboard.LeaderboardEntryDto;
import app.models.Portfolio;
import app.models.User;
import app.repositories.PortfolioRepository;
import app.services.PortfolioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Vérifie le classement par performance (% de gain), la limite demandée et le rang attribué. */
@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private PortfolioService portfolioService;

    private LeaderboardServiceImpl service;

    private Portfolio portfolioOf(String username, BigDecimal startingCapital) {
        User user = new User();
        user.setUsername(username);
        Portfolio portfolio = new Portfolio();
        portfolio.setUser(user);
        portfolio.setStartingCapital(startingCapital);
        return portfolio;
    }

    @Test
    void ranksAccountsByGainPercentDescending() {
        service = new LeaderboardServiceImpl(portfolioRepository, portfolioService);

        Portfolio alice = portfolioOf("alice", new BigDecimal("10000.00")); // +50%
        Portfolio bob = portfolioOf("bob", new BigDecimal("10000.00")); // +10%
        Portfolio carol = portfolioOf("carol", new BigDecimal("10000.00")); // -20%

        when(portfolioRepository.findAllWithUser()).thenReturn(List.of(bob, carol, alice));
        when(portfolioService.getTotalValue(alice)).thenReturn(new BigDecimal("15000.00"));
        when(portfolioService.getTotalValue(bob)).thenReturn(new BigDecimal("11000.00"));
        when(portfolioService.getTotalValue(carol)).thenReturn(new BigDecimal("8000.00"));

        List<LeaderboardEntryDto> leaderboard = service.getLeaderboard(50);

        assertThat(leaderboard).extracting(LeaderboardEntryDto::username).containsExactly("alice", "bob", "carol");
        assertThat(leaderboard).extracting(LeaderboardEntryDto::rank).containsExactly(1, 2, 3);
        assertThat(leaderboard.get(0).gainPercent()).isEqualTo(50.0);
    }

    @Test
    void respectsTheRequestedLimit() {
        service = new LeaderboardServiceImpl(portfolioRepository, portfolioService);

        Portfolio alice = portfolioOf("alice", new BigDecimal("10000.00"));
        Portfolio bob = portfolioOf("bob", new BigDecimal("10000.00"));

        when(portfolioRepository.findAllWithUser()).thenReturn(List.of(alice, bob));
        when(portfolioService.getTotalValue(alice)).thenReturn(new BigDecimal("12000.00"));
        when(portfolioService.getTotalValue(bob)).thenReturn(new BigDecimal("11000.00"));

        List<LeaderboardEntryDto> leaderboard = service.getLeaderboard(1);

        assertThat(leaderboard).hasSize(1);
        assertThat(leaderboard.get(0).username()).isEqualTo("alice");
    }

    @Test
    void zeroStartingCapitalNeverThrowsAndYieldsZeroPercent() {
        service = new LeaderboardServiceImpl(portfolioRepository, portfolioService);

        Portfolio weird = portfolioOf("weird", BigDecimal.ZERO);
        when(portfolioRepository.findAllWithUser()).thenReturn(List.of(weird));
        when(portfolioService.getTotalValue(weird)).thenReturn(new BigDecimal("500.00"));

        List<LeaderboardEntryDto> leaderboard = service.getLeaderboard(50);

        assertThat(leaderboard.get(0).gainPercent()).isEqualTo(0.0);
    }
}
