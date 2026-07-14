package app.services.impl;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.dto.market.PriceQuote;
import app.dto.portfolio.TransactionDto;
import app.enums.TransactionType;
import app.exceptions.InsufficientFundsException;
import app.exceptions.InvalidQuantityException;
import app.models.Holding;
import app.models.Portfolio;
import app.models.User;
import app.repositories.HoldingRepository;
import app.repositories.PortfolioRepository;
import app.repositories.PortfolioValueSnapshotRepository;
import app.repositories.TransactionRepository;
import app.services.market.MarketPriceCacheService;
import jakarta.persistence.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Vérifie la logique de trading portée depuis {@code core/portfolio.py} du
 * jeu de référence : moyenne pondérée à l'achat, fiscalité PFU 30% sur la
 * seule plus-value réalisée, et les gardes-fous (fonds, quantité, ticker).
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PortfolioValueSnapshotRepository portfolioValueSnapshotRepository;
    @Mock
    private MarketPriceCacheService marketPriceCacheService;

    private PortfolioServiceImpl service;
    private User user;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        service = new PortfolioServiceImpl(
            portfolioRepository, holdingRepository, transactionRepository, portfolioValueSnapshotRepository, marketPriceCacheService
        );

        user = new User();
        user.setId(1L);

        portfolio = new Portfolio();
        portfolio.setId(10L);
        portfolio.setUser(user);
        portfolio.setCash(new BigDecimal("10000.00"));
        portfolio.setStartingCapital(new BigDecimal("10000.00"));
        portfolio.setTotalTaxesPaid(BigDecimal.ZERO.setScale(2));
    }

    @Test
    void buyingWithNoExistingHoldingCreatesOneAtTheCurrentPrice() {
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(marketPriceCacheService.getQuote("AAPL")).thenReturn(Optional.of(new PriceQuote("AAPL", 100.0, 0.0)));
        when(holdingRepository.findByPortfolioIdAndTicker(10L, "AAPL")).thenReturn(Optional.empty());
        when(holdingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionDto result = service.buy(user, "AAPL", new BigDecimal("10"));

        assertThat(result.type()).isEqualTo(TransactionType.BUY);
        assertThat(result.price()).isEqualByComparingTo("100.0000");
        assertThat(portfolio.getCash()).isEqualByComparingTo("9000.00");
    }

    @Test
    void buyingMoreOfAnExistingHoldingUpdatesTheWeightedAveragePrice() {
        Holding existing = new Holding();
        existing.setPortfolio(portfolio);
        existing.setTicker("AAPL");
        existing.setQuantity(new BigDecimal("10"));
        existing.setAverageBuyPrice(new BigDecimal("100.0000"));

        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(marketPriceCacheService.getQuote("AAPL")).thenReturn(Optional.of(new PriceQuote("AAPL", 200.0, 0.0)));
        when(holdingRepository.findByPortfolioIdAndTicker(10L, "AAPL")).thenReturn(Optional.of(existing));
        when(holdingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.buy(user, "AAPL", new BigDecimal("10"));

        // (10*100 + 10*200) / 20 = 150
        assertThat(existing.getAverageBuyPrice()).isEqualByComparingTo("150.0000");
        assertThat(existing.getQuantity()).isEqualByComparingTo("20");
    }

    @Test
    void buyingWithInsufficientFundsIsRejected() {
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(marketPriceCacheService.getQuote("AAPL")).thenReturn(Optional.of(new PriceQuote("AAPL", 100.0, 0.0)));

        assertThatThrownBy(() -> service.buy(user, "AAPL", new BigDecimal("1000")))
            .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void buyingAnUnknownTickerIsRejected() {
        assertThatThrownBy(() -> service.buy(user, "NOT-A-TICKER", BigDecimal.TEN))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buyingANonPositiveQuantityIsRejected() {
        assertThatThrownBy(() -> service.buy(user, "AAPL", BigDecimal.ZERO))
            .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void sellingAtAGainAppliesThirtyPercentTaxOnTheGainOnly() {
        Holding holding = new Holding();
        holding.setPortfolio(portfolio);
        holding.setTicker("AAPL");
        holding.setQuantity(new BigDecimal("10"));
        holding.setAverageBuyPrice(new BigDecimal("100.0000"));

        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(marketPriceCacheService.getQuote("AAPL")).thenReturn(Optional.of(new PriceQuote("AAPL", 150.0, 0.0)));
        when(holdingRepository.findByPortfolioIdAndTicker(10L, "AAPL")).thenReturn(Optional.of(holding));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionDto result = service.sell(user, "AAPL", new BigDecimal("10"));

        // plus-value = (150-100)*10 = 500 ; impôt = 500*30% = 150
        assertThat(result.capitalGain()).isEqualByComparingTo("500.00");
        assertThat(result.tax()).isEqualByComparingTo("150.00");
        // cash = 10000 + (10*150) - 150 = 11350
        assertThat(portfolio.getCash()).isEqualByComparingTo("11350.00");
        assertThat(portfolio.getTotalTaxesPaid()).isEqualByComparingTo("150.00");
    }

    @Test
    void sellingAtALossNeverAppliesTax() {
        Holding holding = new Holding();
        holding.setPortfolio(portfolio);
        holding.setTicker("AAPL");
        holding.setQuantity(new BigDecimal("10"));
        holding.setAverageBuyPrice(new BigDecimal("100.0000"));

        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(marketPriceCacheService.getQuote("AAPL")).thenReturn(Optional.of(new PriceQuote("AAPL", 80.0, 0.0)));
        when(holdingRepository.findByPortfolioIdAndTicker(10L, "AAPL")).thenReturn(Optional.of(holding));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionDto result = service.sell(user, "AAPL", new BigDecimal("10"));

        assertThat(result.capitalGain()).isEqualByComparingTo("-200.00");
        assertThat(result.tax()).isEqualByComparingTo("0.00");
    }

    @Test
    void sellingMoreThanHeldIsRejected() {
        Holding holding = new Holding();
        holding.setPortfolio(portfolio);
        holding.setTicker("AAPL");
        holding.setQuantity(new BigDecimal("5"));
        holding.setAverageBuyPrice(new BigDecimal("100.0000"));

        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolioIdAndTicker(10L, "AAPL")).thenReturn(Optional.of(holding));

        assertThatThrownBy(() -> service.sell(user, "AAPL", new BigDecimal("10")))
            .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void sellingATickerNotHeldIsRejected() {
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolioIdAndTicker(10L, "AAPL")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sell(user, "AAPL", BigDecimal.TEN))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
