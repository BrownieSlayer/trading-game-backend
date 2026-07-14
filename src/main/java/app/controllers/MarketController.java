package app.controllers;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.configuration.game.TickerUniverse;
import app.dto.market.MarketStatus;
import app.dto.market.NewsItem;
import app.dto.market.PricePoint;
import app.dto.market.PriceQuote;
import app.services.market.MarketDataService;
import app.services.market.MarketNewsCacheService;
import app.services.market.MarketPriceCacheService;

/**
 * Données de marché : cours en cache, historique, horaires des bourses,
 * actualités financières. Voir {@link MarketPriceCacheService} pour la
 * stratégie de cache des cours (partagée entre tous les utilisateurs).
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketPriceCacheService priceCacheService;
    private final MarketNewsCacheService newsCacheService;
    private final MarketDataService marketDataService;

    public MarketController(
        MarketPriceCacheService priceCacheService,
        MarketNewsCacheService newsCacheService,
        MarketDataService marketDataService
    ) {
        this.priceCacheService = priceCacheService;
        this.newsCacheService = newsCacheService;
        this.marketDataService = marketDataService;
    }

    /** Cours en cache pour les tickers demandés (tout l'univers si non précisé). Un ticker absent de la réponse est temporairement indisponible. */
    @GetMapping("/prices")
    public ResponseEntity<Map<String, PriceQuote>> getPrices(@RequestParam(required = false) List<String> tickers) {
        Collection<String> requested = (tickers == null || tickers.isEmpty())
            ? TickerUniverse.TICKER_BASKET.keySet()
            : tickers;
        return ResponseEntity.ok(priceCacheService.getQuotes(requested));
    }

    /** Historique des cours (en euros) d'un ticker sur la période demandée (ex. "1mo", "1y"). */
    @GetMapping("/history/{ticker}")
    public ResponseEntity<List<PricePoint>> getHistory(
        @PathVariable String ticker,
        @RequestParam(defaultValue = "1y") String period
    ) {
        return marketDataService.getHistory(ticker, period)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Horaires et statut ouvert/fermé des bourses de Paris et New York. */
    @GetMapping("/status")
    public ResponseEntity<List<MarketStatus>> getStatus() {
        return ResponseEntity.ok(marketDataService.getMarketStatus());
    }

    /** Dernières actualités financières générales. */
    @GetMapping("/news")
    public ResponseEntity<List<NewsItem>> getNews(@RequestParam(defaultValue = "3") int limit) {
        return ResponseEntity.ok(newsCacheService.getNews(limit));
    }
}
