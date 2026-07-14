package app.services.market;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import app.configuration.game.TickerGroup;
import app.configuration.game.TickerUniverse;
import app.dto.market.MarketStatus;
import app.dto.market.NewsItem;
import app.dto.market.PricePoint;
import app.dto.market.PriceQuote;

/**
 * Orchestration des données de marché : routage par catégorie de ticker
 * (actions/ETF via Yahoo, crypto via Binance) et conversion en euros.
 * Porté depuis {@code core/market.py} du jeu de référence (Bourse Game).
 */
@Service
public class MarketDataService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private record MarketDefinition(String name, ZoneId zone, LocalTime open, LocalTime close) {}

    private static final List<MarketDefinition> MARKETS = List.of(
        new MarketDefinition("Bourse de Paris", ZoneId.of("Europe/Paris"), LocalTime.of(9, 0), LocalTime.of(17, 30)),
        new MarketDefinition("NYSE / Nasdaq", ZoneId.of("America/New_York"), LocalTime.of(9, 30), LocalTime.of(16, 0))
    );

    private final YahooFinanceClient yahooFinanceClient;
    private final BinanceClient binanceClient;

    public MarketDataService(YahooFinanceClient yahooFinanceClient, BinanceClient binanceClient) {
        this.yahooFinanceClient = yahooFinanceClient;
        this.binanceClient = binanceClient;
    }

    /** Cours (en euros) de tout l'univers de tickers du jeu. */
    public Map<String, PriceQuote> getAllQuotes() {
        return getQuotes(TickerUniverse.TICKER_BASKET.keySet());
    }

    /**
     * Cours (en euros) pour l'ensemble de tickers demandé. Un ticker absent
     * du résultat signifie qu'il est indisponible (erreur réseau, taux de
     * change manquant...) plutôt que d'afficher une valeur incorrecte.
     */
    public Map<String, PriceQuote> getQuotes(Set<String> tickers) {
        Set<String> cryptoTickers = tickers.stream()
            .filter(ticker -> TickerUniverse.groupOf(ticker).filter(g -> g == TickerGroup.CRYPTO).isPresent())
            .collect(Collectors.toSet());
        Set<String> usTickers = tickers.stream()
            .filter(ticker -> TickerUniverse.groupOf(ticker).filter(g -> g == TickerGroup.US).isPresent())
            .collect(Collectors.toSet());
        Set<String> otherStockTickers = tickers.stream()
            .filter(ticker -> !cryptoTickers.contains(ticker) && !usTickers.contains(ticker))
            .collect(Collectors.toSet());

        boolean needsUsdEurRate = !cryptoTickers.isEmpty() || !usTickers.isEmpty();
        Double usdEurRate = needsUsdEurRate ? yahooFinanceClient.fetchUsdEurRate().orElse(null) : null;

        Map<String, PriceQuote> result = new HashMap<>();

        // CAC 40 / ETF : déjà cotés en euros, aucune conversion.
        result.putAll(fetchStockQuotesInParallel(otherStockTickers));

        if (!usTickers.isEmpty()) {
            result.putAll(convertToEur(fetchStockQuotesInParallel(usTickers), usdEurRate));
        }

        if (!cryptoTickers.isEmpty()) {
            result.putAll(convertToEur(binanceClient.fetchQuotes(cryptoTickers), usdEurRate));
        }

        return result;
    }

    private Map<String, PriceQuote> fetchStockQuotesInParallel(Set<String> tickers) {
        if (tickers.isEmpty()) {
            return Map.of();
        }
        List<CompletableFuture<Optional<PriceQuote>>> futures = tickers.stream()
            .map(ticker -> CompletableFuture.supplyAsync(() -> yahooFinanceClient.fetchQuote(ticker)))
            .toList();

        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(Optional::stream)
            .collect(Collectors.toMap(PriceQuote::ticker, quote -> quote));
    }

    /** Convertit des cours USD en euros. Si le taux est indisponible, les tickers concernés sont omis plutôt que mal convertis. */
    private Map<String, PriceQuote> convertToEur(Map<String, PriceQuote> quotesInUsd, Double usdEurRate) {
        if (usdEurRate == null) {
            return Map.of();
        }
        double rate = usdEurRate;
        return quotesInUsd.values().stream()
            .collect(Collectors.toMap(PriceQuote::ticker, q -> new PriceQuote(q.ticker(), q.price() * rate, q.changePercent())));
    }

    /**
     * Historique des cours (en euros) sur `period` (ex. "1y"). Vide si
     * indisponible (ticker invalide, API injoignable, ou taux de change
     * manquant pour un ticker US/Crypto).
     */
    public Optional<List<PricePoint>> getHistory(String ticker, String period) {
        Optional<List<PricePoint>> raw = yahooFinanceClient.fetchHistory(ticker, period);
        if (raw.isEmpty()) {
            return Optional.empty();
        }

        TickerGroup group = TickerUniverse.groupOf(ticker).orElse(null);
        boolean needsConversion = group == TickerGroup.US || group == TickerGroup.CRYPTO;
        if (!needsConversion) {
            return raw;
        }

        Optional<Double> usdEurRate = yahooFinanceClient.fetchUsdEurRate();
        if (usdEurRate.isEmpty()) {
            return Optional.empty();
        }

        double rate = usdEurRate.get();
        return Optional.of(raw.get().stream().map(p -> new PricePoint(p.date(), p.price() * rate)).toList());
    }

    /** Horaires d'ouverture/fermeture (convertis en heure locale de la machine) des places boursières suivies, et si elles sont actuellement ouvertes. */
    public List<MarketStatus> getMarketStatus() {
        ZoneId localZone = ZoneId.systemDefault();
        return MARKETS.stream().map(market -> {
            ZonedDateTime nowAtMarket = ZonedDateTime.now(market.zone());
            boolean isWeekday = nowAtMarket.getDayOfWeek().compareTo(DayOfWeek.SATURDAY) < 0;
            LocalTime currentTime = nowAtMarket.toLocalTime();
            boolean isOpen = isWeekday && !currentTime.isBefore(market.open()) && !currentTime.isAfter(market.close());

            String openLocal = nowAtMarket.with(market.open()).withZoneSameInstant(localZone).format(TIME_FORMAT);
            String closeLocal = nowAtMarket.with(market.close()).withZoneSameInstant(localZone).format(TIME_FORMAT);

            return new MarketStatus(market.name(), openLocal, closeLocal, isOpen);
        }).toList();
    }

    /** Actualités financières générales, les plus récentes en premier. */
    public List<NewsItem> getNews(int limit) {
        return yahooFinanceClient.fetchNews(limit);
    }
}
