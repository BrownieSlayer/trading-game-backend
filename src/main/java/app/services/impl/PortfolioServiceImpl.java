package app.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import app.configuration.game.GameConstants;
import app.configuration.game.TickerUniverse;
import app.dto.market.PriceQuote;
import app.dto.portfolio.HoldingDto;
import app.dto.portfolio.PortfolioDto;
import app.dto.portfolio.PortfolioValuePointDto;
import app.dto.portfolio.TransactionDto;
import app.enums.TransactionType;
import app.exceptions.InsufficientFundsException;
import app.exceptions.InvalidQuantityException;
import app.models.Holding;
import app.models.Portfolio;
import app.models.Transaction;
import app.models.User;
import app.repositories.HoldingRepository;
import app.repositories.PortfolioRepository;
import app.repositories.PortfolioValueSnapshotRepository;
import app.repositories.TransactionRepository;
import app.services.PortfolioService;
import app.services.market.MarketPriceCacheService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private static final int PRICE_SCALE = 4;
    private static final int MONEY_SCALE = 2;

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final PortfolioValueSnapshotRepository portfolioValueSnapshotRepository;
    private final MarketPriceCacheService marketPriceCacheService;

    @Override
    @Transactional
    public Portfolio createPortfolioForUser(User user) {
        BigDecimal startingCapital = BigDecimal.valueOf(GameConstants.STARTING_CAPITAL).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        Portfolio portfolio = new Portfolio();
        portfolio.setUser(user);
        portfolio.setCash(startingCapital);
        portfolio.setStartingCapital(startingCapital);
        portfolio.setTotalTaxesPaid(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        return portfolioRepository.save(portfolio);
    }

    @Override
    public PortfolioDto getPortfolio(User user) {
        Portfolio portfolio = getPortfolioEntity(user);
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolio.getId());

        Set<String> tickers = holdings.stream().map(Holding::getTicker).collect(Collectors.toSet());
        Map<String, PriceQuote> quotes = marketPriceCacheService.getQuotes(tickers);

        BigDecimal holdingsValue = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        List<HoldingDto> holdingDtos = new ArrayList<>();
        for (Holding holding : holdings) {
            BigDecimal currentPrice = priceOrFallback(holding, quotes.get(holding.getTicker()));
            BigDecimal value = holding.getQuantity().multiply(currentPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal costBasis = holding.getQuantity().multiply(holding.getAverageBuyPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal gainAmount = value.subtract(costBasis);
            double gainPercent = percentOf(gainAmount, costBasis);

            holdingsValue = holdingsValue.add(value);
            holdingDtos.add(new HoldingDto(
                holding.getTicker(),
                TickerUniverse.nameOf(holding.getTicker()),
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                currentPrice,
                value,
                gainAmount,
                gainPercent
            ));
        }

        BigDecimal totalValue = portfolio.getCash().add(holdingsValue);
        BigDecimal gainAmount = totalValue.subtract(portfolio.getStartingCapital());
        double gainPercent = percentOf(gainAmount, portfolio.getStartingCapital());

        return new PortfolioDto(
            portfolio.getCash(),
            portfolio.getStartingCapital(),
            portfolio.getTotalTaxesPaid(),
            totalValue,
            gainAmount,
            gainPercent,
            holdingDtos
        );
    }

    @Override
    @Transactional
    public TransactionDto buy(User user, String ticker, BigDecimal quantity) {
        requireKnownTicker(ticker);
        requirePositiveQuantity(quantity);

        Portfolio portfolio = getPortfolioEntity(user);
        BigDecimal price = currentPrice(ticker);
        BigDecimal totalCost = quantity.multiply(price).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (totalCost.compareTo(portfolio.getCash()) > 0) {
            throw new InsufficientFundsException(
                "Fonds insuffisants : il faut %s €, il ne reste que %s €.".formatted(totalCost, portfolio.getCash())
            );
        }

        Holding holding = holdingRepository.findByPortfolioIdAndTicker(portfolio.getId(), ticker).orElse(null);
        if (holding == null) {
            holding = new Holding();
            holding.setPortfolio(portfolio);
            holding.setTicker(ticker);
            holding.setQuantity(quantity);
            holding.setAverageBuyPrice(price);
        } else {
            BigDecimal existingValue = holding.getQuantity().multiply(holding.getAverageBuyPrice());
            BigDecimal newQuantity = holding.getQuantity().add(quantity);
            BigDecimal newAveragePrice = existingValue.add(quantity.multiply(price))
                .divide(newQuantity, PRICE_SCALE, RoundingMode.HALF_UP);
            holding.setQuantity(newQuantity);
            holding.setAverageBuyPrice(newAveragePrice);
        }
        holdingRepository.save(holding);

        portfolio.setCash(portfolio.getCash().subtract(totalCost));
        portfolioRepository.save(portfolio);

        Transaction transaction = new Transaction();
        transaction.setPortfolio(portfolio);
        transaction.setTicker(ticker);
        transaction.setType(TransactionType.BUY);
        transaction.setQuantity(quantity);
        transaction.setPrice(price);
        transaction = transactionRepository.save(transaction);

        return toDto(transaction);
    }

    @Override
    @Transactional
    public TransactionDto sell(User user, String ticker, BigDecimal quantity) {
        requireKnownTicker(ticker);
        requirePositiveQuantity(quantity);

        Portfolio portfolio = getPortfolioEntity(user);
        Holding holding = holdingRepository.findByPortfolioIdAndTicker(portfolio.getId(), ticker)
            .orElseThrow(() -> new EntityNotFoundException("Vous ne possédez aucune position sur " + ticker));

        if (quantity.compareTo(holding.getQuantity()) > 0) {
            throw new InvalidQuantityException(
                "Vous ne possédez que %s unité(s) de %s.".formatted(holding.getQuantity(), ticker)
            );
        }

        BigDecimal price = currentPrice(ticker);
        BigDecimal saleProceeds = quantity.multiply(price).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal capitalGain = price.subtract(holding.getAverageBuyPrice())
            .multiply(quantity)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal tax = capitalGain.signum() > 0
            ? capitalGain.multiply(BigDecimal.valueOf(GameConstants.CAPITAL_GAINS_TAX_RATE)).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
            : BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        portfolio.setCash(portfolio.getCash().add(saleProceeds).subtract(tax));
        portfolio.setTotalTaxesPaid(portfolio.getTotalTaxesPaid().add(tax));
        portfolioRepository.save(portfolio);

        BigDecimal remainingQuantity = holding.getQuantity().subtract(quantity);
        if (remainingQuantity.signum() == 0) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(remainingQuantity);
            holdingRepository.save(holding);
        }

        Transaction transaction = new Transaction();
        transaction.setPortfolio(portfolio);
        transaction.setTicker(ticker);
        transaction.setType(TransactionType.SELL);
        transaction.setQuantity(quantity);
        transaction.setPrice(price);
        transaction.setCapitalGain(capitalGain);
        transaction.setTax(tax);
        transaction = transactionRepository.save(transaction);

        return toDto(transaction);
    }

    @Override
    public List<TransactionDto> getTransactions(User user, int limit) {
        Portfolio portfolio = getPortfolioEntity(user);
        return transactionRepository.findByPortfolioIdOrderByExecutedAtDesc(portfolio.getId(), PageRequest.of(0, limit))
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public BigDecimal getTotalValue(Portfolio portfolio) {
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolio.getId());
        Set<String> tickers = holdings.stream().map(Holding::getTicker).collect(Collectors.toSet());
        Map<String, PriceQuote> quotes = marketPriceCacheService.getQuotes(tickers);

        BigDecimal holdingsValue = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        for (Holding holding : holdings) {
            BigDecimal price = priceOrFallback(holding, quotes.get(holding.getTicker()));
            holdingsValue = holdingsValue.add(holding.getQuantity().multiply(price).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        }
        return portfolio.getCash().add(holdingsValue);
    }

    @Override
    public List<PortfolioValuePointDto> getValueHistory(User user) {
        Portfolio portfolio = getPortfolioEntity(user);
        return portfolioValueSnapshotRepository.findByPortfolioIdOrderBySnapshotDateAsc(portfolio.getId())
            .stream()
            .map(snapshot -> new PortfolioValuePointDto(snapshot.getSnapshotDate(), snapshot.getTotalValue()))
            .toList();
    }

    private BigDecimal priceOrFallback(Holding holding, PriceQuote quote) {
        return quote != null
            ? BigDecimal.valueOf(quote.price()).setScale(PRICE_SCALE, RoundingMode.HALF_UP)
            : holding.getAverageBuyPrice();
    }

    private Portfolio getPortfolioEntity(User user) {
        return portfolioRepository.findByUserId(user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable pour cet utilisateur"));
    }

    private BigDecimal currentPrice(String ticker) {
        return marketPriceCacheService.getQuote(ticker)
            .map(quote -> BigDecimal.valueOf(quote.price()).setScale(PRICE_SCALE, RoundingMode.HALF_UP))
            .orElseThrow(() -> new IllegalStateException("Cours indisponible pour " + ticker + ", réessayez dans quelques instants."));
    }

    private void requireKnownTicker(String ticker) {
        if (!TickerUniverse.isKnown(ticker)) {
            throw new IllegalArgumentException("Ticker inconnu : " + ticker);
        }
    }

    private void requirePositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new InvalidQuantityException("La quantité doit être positive.");
        }
    }

    private double percentOf(BigDecimal amount, BigDecimal base) {
        return base.signum() > 0 ? amount.divide(base, 6, RoundingMode.HALF_UP).doubleValue() * 100 : 0.0;
    }

    private TransactionDto toDto(Transaction transaction) {
        return new TransactionDto(
            transaction.getTicker(),
            transaction.getType(),
            transaction.getQuantity(),
            transaction.getPrice(),
            transaction.getCapitalGain(),
            transaction.getTax(),
            transaction.getExecutedAt()
        );
    }
}
