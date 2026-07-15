package app.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import app.configuration.game.MinigameConstants;
import app.configuration.game.TickerUniverse;
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
import app.services.PariService;
import app.services.market.MarketPriceCacheService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PariServiceImpl implements PariService {

    private static final int PRICE_SCALE = 4;
    private static final int MONEY_SCALE = 2;

    private final PortfolioRepository portfolioRepository;
    private final BetRepository betRepository;
    private final MarketPriceCacheService marketPriceCacheService;
    private final Clock clock;

    @Override
    @Transactional
    public BetDto placeBet(User user, String ticker, BetDirection direction, BigDecimal stake) {
        if (!TickerUniverse.isKnown(ticker)) {
            throw new IllegalArgumentException("Ticker inconnu : " + ticker);
        }
        BigDecimal actualStake = (stake != null ? stake : BigDecimal.valueOf(MinigameConstants.Pari.DEFAULT_STAKE))
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (actualStake.signum() <= 0) {
            throw new InvalidQuantityException("La mise doit être positive.");
        }

        Portfolio portfolio = getPortfolioEntity(user);

        if (betRepository.findByPortfolioIdAndStatus(portfolio.getId(), BetStatus.ACTIVE).isPresent()) {
            throw new IllegalStateException("Un pari est déjà en cours — attends sa résolution.");
        }

        if (actualStake.compareTo(portfolio.getCash()) > 0) {
            throw new InsufficientFundsException(
                "Fonds insuffisants : mise %s €, disponible %s €.".formatted(actualStake, portfolio.getCash())
            );
        }

        BigDecimal referencePrice = currentPrice(ticker);

        portfolio.setCash(portfolio.getCash().subtract(actualStake));
        portfolioRepository.save(portfolio);

        Bet bet = new Bet();
        bet.setPortfolio(portfolio);
        bet.setTicker(ticker);
        bet.setDirection(direction);
        bet.setStake(actualStake);
        bet.setReferencePrice(referencePrice);
        bet.setPlacedDate(LocalDate.now(clock));
        bet.setStatus(BetStatus.ACTIVE);
        bet = betRepository.save(bet);

        return toDto(bet);
    }

    @Override
    public Optional<BetDto> getActiveBet(User user) {
        Portfolio portfolio = getPortfolioEntity(user);
        return betRepository.findByPortfolioIdAndStatus(portfolio.getId(), BetStatus.ACTIVE).map(this::toDto);
    }

    @Override
    @Transactional
    public void resolveActiveBet(Portfolio portfolio, LocalDate today) {
        Optional<Bet> activeBet = betRepository.findByPortfolioIdAndStatus(portfolio.getId(), BetStatus.ACTIVE);
        if (activeBet.isEmpty()) {
            return;
        }
        Bet bet = activeBet.get();

        Optional<PriceQuote> quote = marketPriceCacheService.getQuote(bet.getTicker());
        if (quote.isEmpty()) {
            return; // cours indisponible : on réessaiera à la prochaine résolution
        }

        BigDecimal resultPrice = BigDecimal.valueOf(quote.get().price()).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        boolean actualUp = resultPrice.compareTo(bet.getReferencePrice()) > 0;
        boolean won = (actualUp && bet.getDirection() == BetDirection.UP) || (!actualUp && bet.getDirection() == BetDirection.DOWN);
        BigDecimal gain = won
            ? bet.getStake().multiply(BigDecimal.valueOf(MinigameConstants.Pari.GAIN_RATIO)).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
            : bet.getStake().negate();

        if (won) {
            portfolio.setCash(portfolio.getCash().add(bet.getStake()).add(gain));
            portfolioRepository.save(portfolio);
        }

        bet.setStatus(won ? BetStatus.WON : BetStatus.LOST);
        bet.setResultPrice(resultPrice);
        bet.setGain(gain);
        bet.setResolvedDate(today);
        betRepository.save(bet);
    }

    private BigDecimal currentPrice(String ticker) {
        return marketPriceCacheService.getQuote(ticker)
            .map(quote -> BigDecimal.valueOf(quote.price()).setScale(PRICE_SCALE, RoundingMode.HALF_UP))
            .orElseThrow(() -> new IllegalStateException("Cours indisponible pour " + ticker + ", réessayez dans quelques instants."));
    }

    private Portfolio getPortfolioEntity(User user) {
        return portfolioRepository.findByUserId(user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable pour cet utilisateur"));
    }

    private BetDto toDto(Bet bet) {
        return new BetDto(
            bet.getTicker(), bet.getDirection(), bet.getStake(), bet.getReferencePrice(),
            bet.getPlacedDate(), bet.getStatus(), bet.getResultPrice(), bet.getGain(), bet.getResolvedDate()
        );
    }
}
