package app.services.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import app.models.Portfolio;
import app.models.PortfolioValueSnapshot;
import app.models.User;
import app.repositories.PortfolioRepository;
import app.repositories.PortfolioValueSnapshotRepository;
import app.services.DailyCycleService;
import app.services.PortfolioService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** Port de {@code core/portfolio.py:is_new_day}/{@code update_streak}. */
@Service
@RequiredArgsConstructor
public class DailyCycleServiceImpl implements DailyCycleService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioValueSnapshotRepository portfolioValueSnapshotRepository;
    private final PortfolioService portfolioService;
    private final Clock clock;

    @Override
    @Transactional
    public void resolveNewDay(User user) {
        Portfolio portfolio = portfolioRepository.findByUserId(user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable pour cet utilisateur"));

        LocalDate today = LocalDate.now(clock);
        if (today.equals(portfolio.getLastSessionDate())) {
            return;
        }

        portfolio.setStreak(nextStreak(portfolio.getLastSessionDate(), portfolio.getStreak(), today));
        portfolio.setLastSessionDate(today);
        portfolioRepository.save(portfolio);

        recordValueSnapshot(portfolio, today);
    }

    /**
     * Le jeu n'étant utilisé qu'en semaine, un passage vendredi -> lundi est
     * considéré comme consécutif (pas de rupture de streak le week-end).
     */
    private int nextStreak(LocalDate lastSessionDate, int currentStreak, LocalDate today) {
        if (lastSessionDate == null) {
            return 1;
        }
        long daysDiff = ChronoUnit.DAYS.between(lastSessionDate, today);
        boolean isConsecutive = daysDiff == 1 || (daysDiff == 3 && lastSessionDate.getDayOfWeek() == DayOfWeek.FRIDAY);
        return isConsecutive ? currentStreak + 1 : 1;
    }

    private void recordValueSnapshot(Portfolio portfolio, LocalDate today) {
        if (portfolioValueSnapshotRepository.existsByPortfolioIdAndSnapshotDate(portfolio.getId(), today)) {
            return;
        }
        BigDecimal totalValue = portfolioService.getTotalValue(portfolio);

        PortfolioValueSnapshot snapshot = new PortfolioValueSnapshot();
        snapshot.setPortfolio(portfolio);
        snapshot.setSnapshotDate(today);
        snapshot.setTotalValue(totalValue);
        portfolioValueSnapshotRepository.save(snapshot);
    }
}
