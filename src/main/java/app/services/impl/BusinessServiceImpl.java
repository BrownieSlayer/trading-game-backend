package app.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import app.configuration.game.LeveledUpgrade;
import app.configuration.game.MinigameConstants;
import app.dto.minigames.BusinessDto;
import app.dto.minigames.UpgradeStatusDto;
import app.exceptions.InsufficientFundsException;
import app.exceptions.InvalidQuantityException;
import app.models.BusinessState;
import app.models.Portfolio;
import app.models.User;
import app.repositories.BusinessStateRepository;
import app.repositories.PortfolioRepository;
import app.services.BusinessService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class BusinessServiceImpl implements BusinessService {

    private static final int MONEY_SCALE = 2;

    private final PortfolioRepository portfolioRepository;
    private final BusinessStateRepository businessStateRepository;
    private final Clock clock;
    private final Random random;

    @Autowired
    public BusinessServiceImpl(PortfolioRepository portfolioRepository, BusinessStateRepository businessStateRepository, Clock clock) {
        this(portfolioRepository, businessStateRepository, clock, new Random());
    }

    BusinessServiceImpl(
        PortfolioRepository portfolioRepository, BusinessStateRepository businessStateRepository, Clock clock, Random random
    ) {
        this.portfolioRepository = portfolioRepository;
        this.businessStateRepository = businessStateRepository;
        this.clock = clock;
        this.random = random;
    }

    @Override
    @Transactional
    public BusinessDto getStatus(User user) {
        Portfolio portfolio = getPortfolioEntity(user);
        BusinessState state = getOrCreateState(portfolio);
        harvest(state, portfolio, Instant.now(clock));
        return toDto(state);
    }

    @Override
    @Transactional
    public BusinessDto invest(User user, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidQuantityException("Le montant à investir doit être positif.");
        }

        Portfolio portfolio = getPortfolioEntity(user);
        BusinessState state = getOrCreateState(portfolio);

        if (amount.compareTo(portfolio.getCash()) > 0) {
            throw new InsufficientFundsException(
                "Fonds insuffisants : %s € demandés, %s € disponibles.".formatted(amount, portfolio.getCash())
            );
        }

        BigDecimal cap = investmentCap(state.getCapacityLevel());
        if (state.getInvestment().add(amount).compareTo(cap) > 0) {
            throw new IllegalStateException("Plafond d'investissement atteint (%s €).".formatted(cap));
        }

        harvest(state, portfolio, Instant.now(clock));

        portfolio.setCash(portfolio.getCash().subtract(amount));
        state.setInvestment(state.getInvestment().add(amount));
        portfolioRepository.save(portfolio);
        businessStateRepository.save(state);

        return toDto(state);
    }

    @Override
    @Transactional
    public BusinessDto withdraw(User user, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidQuantityException("Le montant à retirer doit être positif.");
        }

        Portfolio portfolio = getPortfolioEntity(user);
        BusinessState state = getOrCreateState(portfolio);

        if (amount.compareTo(state.getInvestment()) > 0) {
            throw new InvalidQuantityException("Tu n'as que %s € investis.".formatted(state.getInvestment()));
        }

        harvest(state, portfolio, Instant.now(clock));

        state.setInvestment(state.getInvestment().subtract(amount));
        portfolio.setCash(portfolio.getCash().add(amount));
        portfolioRepository.save(portfolio);
        businessStateRepository.save(state);

        return toDto(state);
    }

    @Override
    @Transactional
    public BusinessDto upgrade(User user, String upgradeType) {
        LeveledUpgrade config = MinigameConstants.Business.UPGRADES.get(upgradeType);
        if (config == null) {
            throw new IllegalArgumentException("Amélioration inconnue : " + upgradeType);
        }

        Portfolio portfolio = getPortfolioEntity(user);
        BusinessState state = getOrCreateState(portfolio);

        int currentLevel = levelOf(state, upgradeType);
        if (currentLevel >= config.maxLevel()) {
            throw new IllegalStateException("Niveau maximum déjà atteint pour cette amélioration.");
        }

        BigDecimal cost = BigDecimal.valueOf(config.costForLevel(currentLevel)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (cost.compareTo(portfolio.getCash()) > 0) {
            throw new InsufficientFundsException("Fonds insuffisants : %s € nécessaires.".formatted(cost));
        }

        portfolio.setCash(portfolio.getCash().subtract(cost));
        setLevelOf(state, upgradeType, currentLevel + 1);
        portfolioRepository.save(portfolio);
        businessStateRepository.save(state);

        return toDto(state);
    }

    /**
     * Crédite les revenus horaires accumulés depuis la dernière récolte,
     * heure par heure (aléa indépendant à chaque heure), plafonnés par la
     * limite hors-ligne. Ne fait rien si moins d'une heure pleine ne s'est
     * écoulée. Port de {@code minigames/business.py:recolter}.
     */
    private void harvest(BusinessState state, Portfolio portfolio, Instant now) {
        if (state.getLastHarvestAt() == null || state.getInvestment().signum() <= 0) {
            state.setLastHarvestAt(now);
            businessStateRepository.save(state);
            return;
        }

        long hoursElapsed = Duration.between(state.getLastHarvestAt(), now).toHours();
        if (hoursElapsed <= 0) {
            return;
        }

        long offlineCapHours = offlineHoursLimit(state.getStorageLevel());
        long hoursToCredit = Math.min(hoursElapsed, offlineCapHours);

        double rate = hourlyRate(state.getYieldLevel());
        double totalRaw = 0.0;
        for (long i = 0; i < hoursToCredit; i++) {
            totalRaw += hourlyYieldWithNoise(state.getInvestment().doubleValue(), rate);
        }
        BigDecimal total = BigDecimal.valueOf(totalRaw).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        portfolio.setCash(portfolio.getCash().add(total));
        state.setTotalHarvested(state.getTotalHarvested().add(total));

        if (hoursElapsed > offlineCapHours) {
            state.setLastHarvestAt(now);
        } else {
            state.setLastHarvestAt(state.getLastHarvestAt().plus(hoursElapsed, java.time.temporal.ChronoUnit.HOURS));
        }

        portfolioRepository.save(portfolio);
        businessStateRepository.save(state);
    }

    private double hourlyYieldWithNoise(double investment, double rate) {
        double base = investment * rate * MinigameConstants.Business.coefficientRendement(investment);
        double variation = (random.nextDouble() * 2 - 1) * MinigameConstants.Business.RANDOM_VARIATION;
        return base * (1 + variation);
    }

    private double hourlyRate(int yieldLevel) {
        return MinigameConstants.Business.HOURLY_RATE + yieldLevel * MinigameConstants.Business.UPGRADES.get("rendement").effectPerLevel();
    }

    private BigDecimal investmentCap(int capacityLevel) {
        double cap = MinigameConstants.Business.BASE_MAX_INVESTMENT
            + capacityLevel * MinigameConstants.Business.UPGRADES.get("capacite").effectPerLevel();
        return BigDecimal.valueOf(cap).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private long offlineHoursLimit(int storageLevel) {
        return Math.round(
            MinigameConstants.Business.BASE_OFFLINE_HOURS_LIMIT
                + storageLevel * MinigameConstants.Business.UPGRADES.get("stockage").effectPerLevel()
        );
    }

    private int levelOf(BusinessState state, String upgradeType) {
        return switch (upgradeType) {
            case "capacite" -> state.getCapacityLevel();
            case "rendement" -> state.getYieldLevel();
            case "stockage" -> state.getStorageLevel();
            default -> throw new IllegalArgumentException("Amélioration inconnue : " + upgradeType);
        };
    }

    private void setLevelOf(BusinessState state, String upgradeType, int newLevel) {
        switch (upgradeType) {
            case "capacite" -> state.setCapacityLevel(newLevel);
            case "rendement" -> state.setYieldLevel(newLevel);
            case "stockage" -> state.setStorageLevel(newLevel);
            default -> throw new IllegalArgumentException("Amélioration inconnue : " + upgradeType);
        }
    }

    private Integer minutesUntilNextHarvest(BusinessState state, Instant now) {
        if (state.getInvestment().signum() <= 0 || state.getLastHarvestAt() == null) {
            return null;
        }
        long secondsElapsed = Duration.between(state.getLastHarvestAt(), now).toSeconds();
        long secondsRemaining = 3600 - (secondsElapsed % 3600);
        return (int) Math.max(0, secondsRemaining / 60);
    }

    private BusinessDto toDto(BusinessState state) {
        double rate = hourlyRate(state.getYieldLevel());
        double estimated = state.getInvestment().doubleValue() * rate
            * MinigameConstants.Business.coefficientRendement(state.getInvestment().doubleValue());

        Map<String, UpgradeStatusDto> upgrades = new LinkedHashMap<>();
        for (var entry : MinigameConstants.Business.UPGRADES.entrySet()) {
            String type = entry.getKey();
            LeveledUpgrade config = entry.getValue();
            int level = levelOf(state, type);
            BigDecimal nextCost = level < config.maxLevel()
                ? BigDecimal.valueOf(config.costForLevel(level)).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : null;
            upgrades.put(type, new UpgradeStatusDto(config.label(), level, config.maxLevel(), nextCost));
        }

        return new BusinessDto(
            state.getInvestment(),
            BigDecimal.valueOf(estimated).setScale(MONEY_SCALE, RoundingMode.HALF_UP),
            investmentCap(state.getCapacityLevel()),
            minutesUntilNextHarvest(state, Instant.now(clock)),
            state.getTotalHarvested(),
            upgrades
        );
    }

    private BusinessState getOrCreateState(Portfolio portfolio) {
        return businessStateRepository.findByPortfolioId(portfolio.getId()).orElseGet(() -> {
            BusinessState state = new BusinessState();
            state.setPortfolio(portfolio);
            return businessStateRepository.save(state);
        });
    }

    private Portfolio getPortfolioEntity(User user) {
        return portfolioRepository.findByUserId(user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable pour cet utilisateur"));
    }
}
