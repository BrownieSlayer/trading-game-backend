package app.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import app.dto.leaderboard.LeaderboardEntryDto;
import app.models.Portfolio;
import app.repositories.PortfolioRepository;
import app.services.LeaderboardService;
import app.services.PortfolioService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private record ScoredPortfolio(String username, BigDecimal totalValue, BigDecimal gainAmount, double gainPercent) {}

    private final PortfolioRepository portfolioRepository;
    private final PortfolioService portfolioService;

    @Override
    public List<LeaderboardEntryDto> getLeaderboard(int limit) {
        List<ScoredPortfolio> scored = portfolioRepository.findAllWithUser().stream()
            .map(this::score)
            .sorted(Comparator.comparingDouble(ScoredPortfolio::gainPercent).reversed())
            .limit(limit)
            .toList();

        List<LeaderboardEntryDto> result = new ArrayList<>(scored.size());
        for (int i = 0; i < scored.size(); i++) {
            ScoredPortfolio s = scored.get(i);
            result.add(new LeaderboardEntryDto(i + 1, s.username(), s.totalValue(), s.gainAmount(), s.gainPercent()));
        }
        return result;
    }

    private ScoredPortfolio score(Portfolio portfolio) {
        BigDecimal totalValue = portfolioService.getTotalValue(portfolio);
        BigDecimal gainAmount = totalValue.subtract(portfolio.getStartingCapital());
        double gainPercent = portfolio.getStartingCapital().signum() > 0
            ? gainAmount.divide(portfolio.getStartingCapital(), 6, RoundingMode.HALF_UP).doubleValue() * 100
            : 0.0;
        return new ScoredPortfolio(portfolio.getUser().getUsername(), totalValue, gainAmount, gainPercent);
    }
}
