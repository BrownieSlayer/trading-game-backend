package app.services.market;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import app.dto.market.PriceQuote;

/**
 * Cache serveur partagé des cours de tout l'univers de tickers, rafraîchi
 * toutes les 3 minutes (comme {@code App.AUTO_REFRESH_SECONDS} côté jeu
 * Python de référence). Toutes les requêtes utilisateur lisent ce cache —
 * aucune requête HTTP vers Yahoo/Binance n'est déclenchée par une requête
 * utilisateur individuelle, pour ne pas multiplier les appels externes par
 * le nombre de comptes connectés.
 *
 * Le rafraîchissement met à jour les entrées individuellement plutôt que de
 * remplacer le cache en bloc : un ticker en échec sur un cycle garde sa
 * dernière valeur connue plutôt que de disparaître.
 */
@Service
public class MarketPriceCacheService {

    private static final Logger log = LoggerFactory.getLogger(MarketPriceCacheService.class);
    private static final long REFRESH_RATE_MILLIS = 180_000;

    private final MarketDataService marketDataService;
    private final Map<String, PriceQuote> cache = new ConcurrentHashMap<>();
    private volatile Instant lastRefreshedAt;

    public MarketPriceCacheService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @Scheduled(fixedRate = REFRESH_RATE_MILLIS, initialDelay = 0)
    public void refresh() {
        Map<String, PriceQuote> fresh = marketDataService.getAllQuotes();
        cache.putAll(fresh);
        lastRefreshedAt = Instant.now();
        log.info("Cache de prix rafraîchi : {}/{} tickers disponibles", fresh.size(), cache.size());
    }

    public Optional<PriceQuote> getQuote(String ticker) {
        return Optional.ofNullable(cache.get(ticker));
    }

    public Map<String, PriceQuote> getQuotes(Collection<String> tickers) {
        Map<String, PriceQuote> result = new LinkedHashMap<>();
        for (String ticker : tickers) {
            PriceQuote quote = cache.get(ticker);
            if (quote != null) {
                result.put(ticker, quote);
            }
        }
        return result;
    }

    public Optional<Instant> getLastRefreshedAt() {
        return Optional.ofNullable(lastRefreshedAt);
    }
}
