package app.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import app.configuration.game.MinigameConstants;
import app.dto.minigames.TradingRapideStatusDto;
import app.dto.minigames.TradingSessionDto;
import app.enums.ReflexAction;
import app.enums.ReflexSessionStatus;
import app.models.Portfolio;
import app.models.ReflexTradingSession;
import app.models.User;
import app.repositories.PortfolioRepository;
import app.repositories.ReflexTradingSessionRepository;
import app.services.TradingRapideService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class TradingRapideServiceImpl implements TradingRapideService {

    private static final int PRICE_SCALE = 4;
    private static final int MONEY_SCALE = 2;
    private static final double SEED_PRICE = 100.0;

    private final PortfolioRepository portfolioRepository;
    private final ReflexTradingSessionRepository sessionRepository;
    private final Clock clock;
    private final Random random;

    @Autowired
    public TradingRapideServiceImpl(
        PortfolioRepository portfolioRepository, ReflexTradingSessionRepository sessionRepository, Clock clock
    ) {
        this(portfolioRepository, sessionRepository, clock, new Random());
    }

    TradingRapideServiceImpl(
        PortfolioRepository portfolioRepository, ReflexTradingSessionRepository sessionRepository, Clock clock, Random random
    ) {
        this.portfolioRepository = portfolioRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
        this.random = random;
    }

    @Override
    @Transactional
    public TradingRapideStatusDto start(User user) {
        Portfolio portfolio = getPortfolioEntity(user);
        LocalDate today = LocalDate.now(clock);

        autoResolveStaleSession(portfolio);

        if (sessionRepository.findByPortfolioIdAndStatus(portfolio.getId(), ReflexSessionStatus.OPEN).isPresent()) {
            throw new IllegalStateException("Une partie de trading rapide est déjà en cours.");
        }

        long usedToday = sessionRepository.countByPortfolioIdAndSessionDateAndStatus(
            portfolio.getId(), today, ReflexSessionStatus.RESOLVED
        );
        if (usedToday >= MinigameConstants.TradingRapide.DAILY_ATTEMPTS) {
            throw new IllegalStateException(
                "Plus d'essais aujourd'hui (limite : %d/jour).".formatted(MinigameConstants.TradingRapide.DAILY_ATTEMPTS)
            );
        }

        ReflexTradingSession session = new ReflexTradingSession();
        session.setPortfolio(portfolio);
        session.setSessionDate(today);
        session.setStartedAt(Instant.now(clock));
        session.setPrices(generatePriceWalk());
        session.setStatus(ReflexSessionStatus.OPEN);
        session = sessionRepository.save(session);

        return toStatusDto(session, portfolio, today);
    }

    @Override
    @Transactional
    public TradingRapideStatusDto act(User user, ReflexAction action) {
        Portfolio portfolio = getPortfolioEntity(user);
        LocalDate today = LocalDate.now(clock);

        ReflexTradingSession session = sessionRepository.findByPortfolioIdAndStatus(portfolio.getId(), ReflexSessionStatus.OPEN)
            .orElseThrow(() -> new IllegalStateException("Aucune partie de trading rapide en cours."));

        int tickIndex = currentTickIndex(session.getStartedAt(), Instant.now(clock));

        switch (action) {
            case BUY -> {
                if (session.getBuyTickIndex() != null) {
                    throw new IllegalStateException("Une position est déjà ouverte.");
                }
                session.setBuyTickIndex(tickIndex);
                sessionRepository.save(session);
            }
            case SELL -> {
                if (session.getBuyTickIndex() == null) {
                    throw new IllegalStateException("Aucune position ouverte à vendre.");
                }
                resolveSale(session, portfolio, tickIndex);
            }
            case ABANDON -> resolveAbandon(session);
        }

        return toStatusDto(session, portfolio, today);
    }

    @Override
    public TradingRapideStatusDto getStatus(User user) {
        Portfolio portfolio = getPortfolioEntity(user);
        LocalDate today = LocalDate.now(clock);

        autoResolveStaleSession(portfolio);

        ReflexTradingSession openSession = sessionRepository
            .findByPortfolioIdAndStatus(portfolio.getId(), ReflexSessionStatus.OPEN)
            .orElse(null);
        return toStatusDto(openSession, portfolio, today);
    }

    /** Vente automatique au dernier cours si une position était encore ouverte quand le temps s'est écoulé (jamais en cas d'abandon volontaire, déjà résolu). */
    private void autoResolveStaleSession(Portfolio portfolio) {
        Optional<ReflexTradingSession> openOpt = sessionRepository.findByPortfolioIdAndStatus(portfolio.getId(), ReflexSessionStatus.OPEN);
        if (openOpt.isEmpty()) {
            return;
        }
        ReflexTradingSession session = openOpt.get();
        Instant now = Instant.now(clock);
        double elapsedSeconds = Duration.between(session.getStartedAt(), now).toMillis() / 1000.0;
        if (elapsedSeconds < MinigameConstants.TradingRapide.DURATION_SECONDS) {
            return;
        }

        if (session.getBuyTickIndex() != null) {
            resolveSale(session, portfolio, MinigameConstants.TradingRapide.TICK_COUNT - 1);
        } else {
            session.setGain(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            session.setStatus(ReflexSessionStatus.RESOLVED);
            sessionRepository.save(session);
        }
    }

    private void resolveSale(ReflexTradingSession session, Portfolio portfolio, int sellTickIndex) {
        BigDecimal buyPrice = session.getPrices().get(session.getBuyTickIndex());
        BigDecimal sellPrice = session.getPrices().get(sellTickIndex);
        BigDecimal gain = computeGain(buyPrice, sellPrice);

        session.setSellTickIndex(sellTickIndex);
        session.setGain(gain);
        session.setStatus(ReflexSessionStatus.RESOLVED);
        sessionRepository.save(session);

        portfolio.setCash(portfolio.getCash().add(gain));
        portfolioRepository.save(portfolio);
    }

    private void resolveAbandon(ReflexTradingSession session) {
        session.setGain(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        session.setAbandoned(true);
        session.setStatus(ReflexSessionStatus.RESOLVED);
        sessionRepository.save(session);
        // Aucune position vendue : rien à créditer/débiter au portefeuille.
    }

    private List<BigDecimal> generatePriceWalk() {
        List<BigDecimal> prices = new ArrayList<>(MinigameConstants.TradingRapide.TICK_COUNT);
        double price = SEED_PRICE;
        for (int i = 0; i < MinigameConstants.TradingRapide.TICK_COUNT; i++) {
            double variation = random.nextGaussian() * MinigameConstants.TradingRapide.VOLATILITY;
            price = Math.max(0.01, price * (1 + variation));
            prices.add(BigDecimal.valueOf(price).setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        }
        return prices;
    }

    /** Gain/perte € pour un aller-retour achat/vente, sur la mise notionnelle du config. */
    static BigDecimal computeGain(BigDecimal buyPrice, BigDecimal sellPrice) {
        BigDecimal variation = sellPrice.subtract(buyPrice).divide(buyPrice, 10, RoundingMode.HALF_UP);
        return variation.multiply(BigDecimal.valueOf(MinigameConstants.TradingRapide.NOTIONAL_STAKE)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Index du tick réellement écoulé depuis `startedAt`, revalidé côté serveur — jamais fourni par le client. */
    static int currentTickIndex(Instant startedAt, Instant now) {
        long elapsedMillis = Duration.between(startedAt, now).toMillis();
        long tickIntervalMillis = Math.round(MinigameConstants.TradingRapide.TICK_INTERVAL_SECONDS * 1000);
        int index = (int) (elapsedMillis / tickIntervalMillis);
        return Math.min(Math.max(index, 0), MinigameConstants.TradingRapide.TICK_COUNT - 1);
    }

    private TradingRapideStatusDto toStatusDto(ReflexTradingSession session, Portfolio portfolio, LocalDate today) {
        long usedToday = sessionRepository.countByPortfolioIdAndSessionDateAndStatus(
            portfolio.getId(), today, ReflexSessionStatus.RESOLVED
        );
        int remaining = (int) Math.max(0, MinigameConstants.TradingRapide.DAILY_ATTEMPTS - usedToday);
        TradingSessionDto sessionDto = session != null ? toSessionDto(session) : null;
        return new TradingRapideStatusDto(sessionDto, remaining);
    }

    private TradingSessionDto toSessionDto(ReflexTradingSession session) {
        return new TradingSessionDto(
            session.getId(),
            session.getPrices(),
            session.getStartedAt(),
            session.getBuyTickIndex(),
            session.getSellTickIndex(),
            session.getGain(),
            session.isAbandoned(),
            session.getStatus()
        );
    }

    private Portfolio getPortfolioEntity(User user) {
        return portfolioRepository.findByUserId(user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable pour cet utilisateur"));
    }
}
