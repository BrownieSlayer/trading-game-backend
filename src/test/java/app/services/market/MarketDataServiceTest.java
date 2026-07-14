package app.services.market;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.dto.market.PricePoint;
import app.dto.market.PriceQuote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Vérifie le routage par catégorie de ticker (CAC 40/ETF déjà en euros, US
 * et Crypto convertis) et la règle "taux indisponible => ticker omis plutôt
 * que mal converti", portée depuis {@code core/market.py} du jeu de
 * référence.
 */
@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private YahooFinanceClient yahooFinanceClient;

    @Mock
    private BinanceClient binanceClient;

    private MarketDataService marketDataService() {
        return new MarketDataService(yahooFinanceClient, binanceClient);
    }

    @Test
    void cac40TickerIsReturnedAsIsWithoutCurrencyConversion() {
        when(yahooFinanceClient.fetchQuote("AI.PA")).thenReturn(Optional.of(new PriceQuote("AI.PA", 180.0, 1.5)));

        Map<String, PriceQuote> result = marketDataService().getQuotes(Set.of("AI.PA"));

        assertThat(result).containsEntry("AI.PA", new PriceQuote("AI.PA", 180.0, 1.5));
        verify(yahooFinanceClient, never()).fetchUsdEurRate();
    }

    @Test
    void usTickerIsConvertedToEurUsingTheCurrentRate() {
        when(yahooFinanceClient.fetchQuote("AAPL")).thenReturn(Optional.of(new PriceQuote("AAPL", 100.0, 2.0)));
        when(yahooFinanceClient.fetchUsdEurRate()).thenReturn(Optional.of(0.9));

        Map<String, PriceQuote> result = marketDataService().getQuotes(Set.of("AAPL"));

        assertThat(result.get("AAPL").price()).isEqualTo(90.0);
        assertThat(result.get("AAPL").changePercent()).isEqualTo(2.0);
    }

    @Test
    void usTickerIsOmittedWhenExchangeRateIsUnavailable() {
        when(yahooFinanceClient.fetchQuote("AAPL")).thenReturn(Optional.of(new PriceQuote("AAPL", 100.0, 2.0)));
        when(yahooFinanceClient.fetchUsdEurRate()).thenReturn(Optional.empty());

        Map<String, PriceQuote> result = marketDataService().getQuotes(Set.of("AAPL"));

        assertThat(result).isEmpty();
    }

    @Test
    void cryptoTickerIsRoutedToBinanceAndConvertedToEur() {
        when(binanceClient.fetchQuotes(Set.of("BTC-USD")))
            .thenReturn(Map.of("BTC-USD", new PriceQuote("BTC-USD", 50_000.0, -1.0)));
        when(yahooFinanceClient.fetchUsdEurRate()).thenReturn(Optional.of(0.9));

        Map<String, PriceQuote> result = marketDataService().getQuotes(Set.of("BTC-USD"));

        assertThat(result.get("BTC-USD").price()).isEqualTo(45_000.0);
        verify(yahooFinanceClient, never()).fetchQuote("BTC-USD");
    }

    @Test
    void cac40HistoryIsReturnedWithoutConversion() {
        List<PricePoint> raw = List.of(new PricePoint(java.time.LocalDate.of(2026, 1, 1), 100.0));
        when(yahooFinanceClient.fetchHistory("AI.PA", "1y")).thenReturn(Optional.of(raw));

        Optional<List<PricePoint>> result = marketDataService().getHistory("AI.PA", "1y");

        assertThat(result).contains(raw);
        verify(yahooFinanceClient, never()).fetchUsdEurRate();
    }

    @Test
    void usHistoryIsConvertedToEur() {
        List<PricePoint> raw = List.of(new PricePoint(java.time.LocalDate.of(2026, 1, 1), 100.0));
        when(yahooFinanceClient.fetchHistory("AAPL", "1y")).thenReturn(Optional.of(raw));
        when(yahooFinanceClient.fetchUsdEurRate()).thenReturn(Optional.of(0.9));

        Optional<List<PricePoint>> result = marketDataService().getHistory("AAPL", "1y");

        assertThat(result).isPresent();
        assertThat(result.get().get(0).price()).isEqualTo(90.0);
    }

    @Test
    void usHistoryIsUnavailableWhenExchangeRateIsMissing() {
        List<PricePoint> raw = List.of(new PricePoint(java.time.LocalDate.of(2026, 1, 1), 100.0));
        when(yahooFinanceClient.fetchHistory("AAPL", "1y")).thenReturn(Optional.of(raw));
        when(yahooFinanceClient.fetchUsdEurRate()).thenReturn(Optional.empty());

        Optional<List<PricePoint>> result = marketDataService().getHistory("AAPL", "1y");

        assertThat(result).isEmpty();
    }

    @Test
    void marketStatusReturnsParisAndNewYorkWithFormattedLocalTimes() {
        var statuses = marketDataService().getMarketStatus();

        assertThat(statuses).hasSize(2);
        assertThat(statuses).extracting(s -> s.name()).containsExactly("Bourse de Paris", "NYSE / Nasdaq");
        assertThat(statuses).allSatisfy(status -> {
            assertThat(status.openLocal()).matches("\\d{2}:\\d{2}");
            assertThat(status.closeLocal()).matches("\\d{2}:\\d{2}");
        });
    }
}
