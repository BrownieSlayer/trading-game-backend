package app.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import app.configuration.game.EmploiTier;
import app.configuration.game.LeveledUpgrade;
import app.configuration.game.MinigameConstants;
import app.dto.minigames.ActiveJobDto;
import app.dto.minigames.EmploymentDto;
import app.dto.minigames.JobOfferDto;
import app.dto.minigames.UpgradeStatusDto;
import app.exceptions.InsufficientFundsException;
import app.models.EmploymentState;
import app.models.JobOffer;
import app.models.Portfolio;
import app.models.User;
import app.repositories.EmploymentStateRepository;
import app.repositories.JobOfferRepository;
import app.repositories.PortfolioRepository;
import app.services.EmploymentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class EmploymentServiceImpl implements EmploymentService {

    private static final int MONEY_SCALE = 2;

    private final PortfolioRepository portfolioRepository;
    private final EmploymentStateRepository employmentStateRepository;
    private final JobOfferRepository jobOfferRepository;
    private final Clock clock;
    private final Random random;

    @Autowired
    public EmploymentServiceImpl(
        PortfolioRepository portfolioRepository,
        EmploymentStateRepository employmentStateRepository,
        JobOfferRepository jobOfferRepository,
        Clock clock
    ) {
        this(portfolioRepository, employmentStateRepository, jobOfferRepository, clock, new Random());
    }

    EmploymentServiceImpl(
        PortfolioRepository portfolioRepository,
        EmploymentStateRepository employmentStateRepository,
        JobOfferRepository jobOfferRepository,
        Clock clock,
        Random random
    ) {
        this.portfolioRepository = portfolioRepository;
        this.employmentStateRepository = employmentStateRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.clock = clock;
        this.random = random;
    }

    @Override
    @Transactional
    public EmploymentDto getStatus(User user) {
        Portfolio portfolio = getPortfolioEntity(user);
        EmploymentState state = getOrCreateState(portfolio);
        checkJobCompletion(state, portfolio, Instant.now(clock));
        return toDto(state, portfolio.getId());
    }

    @Override
    @Transactional
    public List<JobOfferDto> searchOffers(User user) {
        Portfolio portfolio = getPortfolioEntity(user);
        EmploymentState state = getOrCreateState(portfolio);
        checkJobCompletion(state, portfolio, Instant.now(clock));

        if (state.getActiveJobEndsAt() != null) {
            throw new IllegalStateException("Un poste est déjà en cours — termine-le avant de chercher un nouvel emploi.");
        }

        jobOfferRepository.deleteByPortfolioId(portfolio.getId());

        int totalSkillLevel = state.getManualLevel() + state.getIntellectLevel() + state.getComputerLevel();
        List<JobOfferDto> offers = new java.util.ArrayList<>();
        for (int i = 0; i < MinigameConstants.Emploi.OFFER_COUNT; i++) {
            JobOffer offer = generateOffer(state, portfolio, totalSkillLevel);
            offer = jobOfferRepository.save(offer);
            offers.add(toOfferDto(offer, i));
        }
        return offers;
    }

    @Override
    @Transactional
    public EmploymentDto acceptOffer(User user, int offerIndex) {
        Portfolio portfolio = getPortfolioEntity(user);
        EmploymentState state = getOrCreateState(portfolio);
        checkJobCompletion(state, portfolio, Instant.now(clock));

        if (state.getActiveJobEndsAt() != null) {
            throw new IllegalStateException("Un poste est déjà en cours.");
        }

        List<JobOffer> offers = jobOfferRepository.findByPortfolioIdOrderByIdAsc(portfolio.getId());
        if (offerIndex < 0 || offerIndex >= offers.size()) {
            throw new IllegalArgumentException("Offre invalide.");
        }
        JobOffer accepted = offers.get(offerIndex);

        Instant now = Instant.now(clock);
        state.setActiveJobTitle(accepted.getTitle());
        state.setActiveJobTier(accepted.getTier());
        state.setActiveJobDurationHours(accepted.getDurationHours());
        state.setActiveJobStartedAt(now);
        state.setActiveJobEndsAt(now.plus(accepted.getDurationHours(), java.time.temporal.ChronoUnit.HOURS));
        state.setActiveJobHourlyWage(accepted.getHourlyWage());
        state.setActiveJobTotalWage(accepted.getTotalWage());
        employmentStateRepository.save(state);

        jobOfferRepository.deleteByPortfolioId(portfolio.getId());

        return toDto(state, portfolio.getId());
    }

    @Override
    @Transactional
    public EmploymentDto upgradeSkill(User user, String skill) {
        LeveledUpgrade config = MinigameConstants.Emploi.SKILLS.get(skill);
        if (config == null) {
            throw new IllegalArgumentException("Compétence inconnue : " + skill);
        }

        Portfolio portfolio = getPortfolioEntity(user);
        EmploymentState state = getOrCreateState(portfolio);

        int currentLevel = levelOf(state, skill);
        if (currentLevel >= config.maxLevel()) {
            throw new IllegalStateException("Niveau maximum déjà atteint pour cette compétence.");
        }

        BigDecimal cost = BigDecimal.valueOf(config.costForLevel(currentLevel)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (cost.compareTo(portfolio.getCash()) > 0) {
            throw new InsufficientFundsException("Fonds insuffisants : %s € nécessaires.".formatted(cost));
        }

        portfolio.setCash(portfolio.getCash().subtract(cost));
        setLevelOf(state, skill, currentLevel + 1);
        portfolioRepository.save(portfolio);
        employmentStateRepository.save(state);

        return toDto(state, portfolio.getId());
    }

    /** Crédite le poste actif si son échéance est dépassée. Port de {@code minigames/emploi.py:verifier_poste}. */
    private void checkJobCompletion(EmploymentState state, Portfolio portfolio, Instant now) {
        if (state.getActiveJobEndsAt() == null || now.isBefore(state.getActiveJobEndsAt())) {
            return;
        }

        BigDecimal wage = state.getActiveJobTotalWage();
        portfolio.setCash(portfolio.getCash().add(wage));
        state.setTotalEarned(state.getTotalEarned().add(wage));

        state.setActiveJobTitle(null);
        state.setActiveJobTier(null);
        state.setActiveJobDurationHours(null);
        state.setActiveJobStartedAt(null);
        state.setActiveJobEndsAt(null);
        state.setActiveJobHourlyWage(null);
        state.setActiveJobTotalWage(null);

        portfolioRepository.save(portfolio);
        employmentStateRepository.save(state);
    }

    private JobOffer generateOffer(EmploymentState state, Portfolio portfolio, int totalSkillLevel) {
        String tier = drawTier(MinigameConstants.Emploi.TIERS, totalSkillLevel, random.nextInt(totalTierWeight(totalSkillLevel)));
        EmploiTier tierConfig = MinigameConstants.Emploi.TIERS.get(tier);
        List<String> titles = MinigameConstants.Emploi.JOB_TITLES.get(tier);
        String title = titles.get(random.nextInt(titles.size()));
        int durationHours = MinigameConstants.Emploi.OFFER_DURATIONS_HOURS.get(
            random.nextInt(MinigameConstants.Emploi.OFFER_DURATIONS_HOURS.size())
        );

        double baseWage = tierConfig.minHourlyWage() + random.nextDouble() * (tierConfig.maxHourlyWage() - tierConfig.minHourlyWage());
        double wage = baseWage + skillBonusTotal(state);
        double variation = (random.nextDouble() * 2 - 1) * MinigameConstants.Emploi.WAGE_RANDOM_VARIATION;
        wage = Math.max(0.5, wage * (1 + variation));

        BigDecimal hourlyWage = BigDecimal.valueOf(wage).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalWage = hourlyWage.multiply(BigDecimal.valueOf(durationHours)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        JobOffer offer = new JobOffer();
        offer.setPortfolio(portfolio);
        offer.setTitle(title);
        offer.setTier(tier);
        offer.setDurationHours(durationHours);
        offer.setHourlyWage(hourlyWage);
        offer.setTotalWage(totalWage);
        return offer;
    }

    private double skillBonusTotal(EmploymentState state) {
        return state.getManualLevel() * MinigameConstants.Emploi.SKILLS.get("manuel").effectPerLevel()
            + state.getIntellectLevel() * MinigameConstants.Emploi.SKILLS.get("intellect").effectPerLevel()
            + state.getComputerLevel() * MinigameConstants.Emploi.SKILLS.get("informatique").effectPerLevel();
    }

    private int totalTierWeight(int totalSkillLevel) {
        int total = tierWeights(MinigameConstants.Emploi.TIERS, totalSkillLevel).values().stream().mapToInt(Integer::intValue).sum();
        return Math.max(total, 1); // évite un random.nextInt(0) si toutes les pondérations sont nulles
    }

    static Map<String, Integer> tierWeights(Map<String, EmploiTier> tiers, int totalSkillLevel) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        for (var entry : tiers.entrySet()) {
            EmploiTier config = entry.getValue();
            int weight = (int) Math.max(0, Math.round(config.baseWeight() + config.weightPerSkillLevel() * totalSkillLevel));
            weights.put(entry.getKey(), weight);
        }
        return weights;
    }

    /** Tire le palier correspondant à `roll` par cumul des poids — fonction pure, testable indépendamment du générateur aléatoire. */
    static String drawTier(Map<String, EmploiTier> tiers, int totalSkillLevel, int roll) {
        Map<String, Integer> weights = tierWeights(tiers, totalSkillLevel);
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            return "nul";
        }
        int cumulative = 0;
        for (var entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }
        return "nul";
    }

    private int levelOf(EmploymentState state, String skill) {
        return switch (skill) {
            case "manuel" -> state.getManualLevel();
            case "intellect" -> state.getIntellectLevel();
            case "informatique" -> state.getComputerLevel();
            default -> throw new IllegalArgumentException("Compétence inconnue : " + skill);
        };
    }

    private void setLevelOf(EmploymentState state, String skill, int newLevel) {
        switch (skill) {
            case "manuel" -> state.setManualLevel(newLevel);
            case "intellect" -> state.setIntellectLevel(newLevel);
            case "informatique" -> state.setComputerLevel(newLevel);
            default -> throw new IllegalArgumentException("Compétence inconnue : " + skill);
        }
    }

    private EmploymentDto toDto(EmploymentState state, Long portfolioId) {
        Map<String, UpgradeStatusDto> skills = new LinkedHashMap<>();
        for (var entry : MinigameConstants.Emploi.SKILLS.entrySet()) {
            String key = entry.getKey();
            LeveledUpgrade config = entry.getValue();
            int level = levelOf(state, key);
            BigDecimal nextCost = level < config.maxLevel()
                ? BigDecimal.valueOf(config.costForLevel(level)).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : null;
            skills.put(key, new UpgradeStatusDto(config.label(), level, config.maxLevel(), nextCost));
        }

        ActiveJobDto activeJob = null;
        List<JobOfferDto> offers = List.of();
        if (state.getActiveJobEndsAt() != null) {
            long minutesRemaining = Math.max(0, Duration.between(Instant.now(clock), state.getActiveJobEndsAt()).toMinutes());
            activeJob = new ActiveJobDto(
                state.getActiveJobTitle(),
                state.getActiveJobTier(),
                state.getActiveJobDurationHours(),
                state.getActiveJobStartedAt(),
                state.getActiveJobEndsAt(),
                state.getActiveJobHourlyWage(),
                state.getActiveJobTotalWage(),
                (int) minutesRemaining
            );
        } else {
            List<JobOffer> currentOffers = jobOfferRepository.findByPortfolioIdOrderByIdAsc(portfolioId);
            offers = new java.util.ArrayList<>();
            for (int i = 0; i < currentOffers.size(); i++) {
                offers.add(toOfferDto(currentOffers.get(i), i));
            }
        }

        return new EmploymentDto(state.getTotalEarned(), skills, offers, activeJob);
    }

    private JobOfferDto toOfferDto(JobOffer offer, int index) {
        return new JobOfferDto(index, offer.getTitle(), offer.getTier(), offer.getDurationHours(), offer.getHourlyWage(), offer.getTotalWage());
    }

    private EmploymentState getOrCreateState(Portfolio portfolio) {
        return employmentStateRepository.findByPortfolioId(portfolio.getId()).orElseGet(() -> {
            EmploymentState state = new EmploymentState();
            state.setPortfolio(portfolio);
            return employmentStateRepository.save(state);
        });
    }

    private Portfolio getPortfolioEntity(User user) {
        return portfolioRepository.findByUserId(user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable pour cet utilisateur"));
    }
}
