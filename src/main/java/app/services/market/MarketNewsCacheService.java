package app.services.market;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import app.dto.market.NewsItem;

/**
 * Cache des actualités financières, rafraîchi toutes les 20 minutes — les
 * news changent peu vite pour justifier un appel réseau aussi fréquent que
 * les cours (même choix que côté jeu Python de référence, où les news ne
 * sont rafraîchies qu'au clic sur "Actualiser").
 */
@Service
public class MarketNewsCacheService {

    private static final long REFRESH_RATE_MILLIS = 1_200_000;
    private static final int CACHE_SIZE = 10;

    private final MarketDataService marketDataService;
    private volatile List<NewsItem> cachedNews = List.of();

    public MarketNewsCacheService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @Scheduled(fixedRate = REFRESH_RATE_MILLIS, initialDelay = 0)
    public void refresh() {
        cachedNews = marketDataService.getNews(CACHE_SIZE);
    }

    public List<NewsItem> getNews(int limit) {
        return cachedNews.stream().limit(limit).toList();
    }
}
