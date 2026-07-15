package app.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import app.configuration.game.MinigameConstants;
import app.configuration.game.WheelResult;
import app.dto.minigames.WheelSpinDto;
import app.models.Portfolio;
import app.models.User;
import app.models.WheelSpin;
import app.repositories.PortfolioRepository;
import app.repositories.WheelSpinRepository;
import app.services.RoueService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class RoueServiceImpl implements RoueService {

    private static final int MONEY_SCALE = 2;

    private final PortfolioRepository portfolioRepository;
    private final WheelSpinRepository wheelSpinRepository;
    private final Clock clock;
    private final Random random;

    @Autowired
    public RoueServiceImpl(PortfolioRepository portfolioRepository, WheelSpinRepository wheelSpinRepository, Clock clock) {
        this(portfolioRepository, wheelSpinRepository, clock, new Random());
    }

    RoueServiceImpl(PortfolioRepository portfolioRepository, WheelSpinRepository wheelSpinRepository, Clock clock, Random random) {
        this.portfolioRepository = portfolioRepository;
        this.wheelSpinRepository = wheelSpinRepository;
        this.clock = clock;
        this.random = random;
    }

    @Override
    @Transactional
    public WheelSpinDto spin(User user) {
        Portfolio portfolio = getPortfolioEntity(user);
        LocalDate today = LocalDate.now(clock);

        if (wheelSpinRepository.existsByPortfolioIdAndSpinDate(portfolio.getId(), today)) {
            throw new IllegalStateException("Tu as déjà fait ton tirage aujourd'hui.");
        }

        List<WheelResult> results = MinigameConstants.Roue.RESULTS;
        int totalWeight = results.stream().mapToInt(WheelResult::weight).sum();
        WheelResult drawn = pickResult(results, random.nextInt(totalWeight));

        BigDecimal gain = BigDecimal.valueOf(drawn.gain()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        portfolio.setCash(portfolio.getCash().add(gain));
        portfolioRepository.save(portfolio);

        WheelSpin spin = new WheelSpin();
        spin.setPortfolio(portfolio);
        spin.setSpinDate(today);
        spin.setLabel(drawn.label());
        spin.setGain(gain);
        spin = wheelSpinRepository.save(spin);

        return toDto(spin);
    }

    @Override
    public Optional<WheelSpinDto> getTodayResult(User user) {
        Portfolio portfolio = getPortfolioEntity(user);
        return wheelSpinRepository.findByPortfolioIdAndSpinDate(portfolio.getId(), LocalDate.now(clock)).map(this::toDto);
    }

    /**
     * Sélectionne le résultat correspondant à `roll` (0 inclus, `totalWeight`
     * exclu) dans la table pondérée, par cumul des poids — fonction pure,
     * testable indépendamment du générateur aléatoire.
     */
    static WheelResult pickResult(List<WheelResult> results, int roll) {
        int cumulative = 0;
        for (WheelResult candidate : results) {
            cumulative += candidate.weight();
            if (roll < cumulative) {
                return candidate;
            }
        }
        return results.get(results.size() - 1);
    }

    private Portfolio getPortfolioEntity(User user) {
        return portfolioRepository.findByUserId(user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable pour cet utilisateur"));
    }

    private WheelSpinDto toDto(WheelSpin spin) {
        return new WheelSpinDto(spin.getLabel(), spin.getGain(), spin.getSpinDate());
    }
}
