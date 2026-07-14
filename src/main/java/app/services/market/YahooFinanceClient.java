package app.services.market;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

import app.dto.market.NewsItem;
import app.dto.market.PricePoint;
import app.dto.market.PriceQuote;

/**
 * Client pour les endpoints HTTP non officiels de Yahoo Finance (les mêmes
 * que ceux utilisés en interne par la bibliothèque Python {@code yfinance}
 * du jeu de référence) : cours/historique via l'API "chart", actualités via
 * l'API "search". Aucune clé API, mais endpoints non documentés et sujets à
 * changement sans préavis — toute erreur de parsing/réseau est absorbée et
 * traduite en résultat vide plutôt que de remonter une exception, pour ne
 * jamais faire tomber le rafraîchissement du cache de prix à cause d'un
 * ticker ou d'une réponse imprévue.
 */
@Component
public class YahooFinanceClient {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceClient.class);

    private static final String CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart/{ticker}";
    private static final String SEARCH_URL = "https://query1.finance.yahoo.com/v1/finance/search";

    private final RestClient restClient;

    public YahooFinanceClient(RestClient marketDataRestClient) {
        this.restClient = marketDataRestClient;
    }

    /** Dernier cours connu et variation % vs la clôture précédente, dans la devise native du ticker. */
    public Optional<PriceQuote> fetchQuote(String ticker) {
        return fetchCloseSeries(ticker, "5d").flatMap(series -> {
            if (series.isEmpty()) {
                return Optional.empty();
            }
            double last = series.get(series.size() - 1).price();
            double changePercent = 0.0;
            if (series.size() >= 2) {
                double previous = series.get(series.size() - 2).price();
                changePercent = previous != 0.0 ? (last - previous) / previous * 100 : 0.0;
            }
            return Optional.of(new PriceQuote(ticker, last, changePercent));
        });
    }

    /** Historique des clôtures sur `period` (ex. "1y"), du plus ancien au plus récent, dans la devise native du ticker. */
    public Optional<List<PricePoint>> fetchHistory(String ticker, String period) {
        return fetchCloseSeries(ticker, period);
    }

    private Optional<List<PricePoint>> fetchCloseSeries(String ticker, String range) {
        try {
            JsonNode root = restClient.get()
                .uri(CHART_URL + "?range={range}&interval=1d", ticker, range)
                .retrieve()
                .body(JsonNode.class);

            return Optional.ofNullable(root)
                .map(this::extractCloseSeries)
                .filter(series -> !series.isEmpty());
        } catch (Exception e) {
            log.warn("Yahoo Finance: échec de récupération du cours pour {} ({})", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    private List<PricePoint> extractCloseSeries(JsonNode root) {
        JsonNode result = root.path("chart").path("result");
        if (!result.isArray() || result.isEmpty()) {
            return List.of();
        }
        JsonNode first = result.get(0);
        JsonNode timestamps = first.path("timestamp");
        JsonNode closes = first.path("indicators").path("quote").path(0).path("close");

        List<PricePoint> series = new ArrayList<>();
        for (int i = 0; i < timestamps.size() && i < closes.size(); i++) {
            JsonNode closeNode = closes.get(i);
            if (closeNode == null || closeNode.isNull()) {
                continue;
            }
            LocalDate date = Instant.ofEpochSecond(timestamps.get(i).asLong()).atZone(ZoneOffset.UTC).toLocalDate();
            series.add(new PricePoint(date, closeNode.asDouble()));
        }
        return series;
    }

    /** Taux de change USD -> EUR (paire forex "USDEUR=X"), ou vide si indisponible. */
    public Optional<Double> fetchUsdEurRate() {
        return fetchQuote("USDEUR=X").map(PriceQuote::price);
    }

    /**
     * Actualités financières générales (marché large, pas liées à un ticker
     * du panier) — endpoint "search" de Yahoo interrogé sur l'indice S&P 500,
     * même source que {@code yf.Ticker("^GSPC").news} côté Python. Liste
     * vide si indisponible (erreur réseau, pas de news, forme de réponse
     * inattendue).
     */
    public List<NewsItem> fetchNews(int limit) {
        try {
            JsonNode root = restClient.get()
                .uri(SEARCH_URL + "?q={q}&newsCount={count}&quotesCount=0", "^GSPC", limit)
                .retrieve()
                .body(JsonNode.class);

            if (root == null) {
                return List.of();
            }

            List<NewsItem> news = new ArrayList<>();
            for (JsonNode item : root.path("news")) {
                String title = item.path("title").asText(null);
                if (title == null || title.isBlank()) {
                    continue;
                }
                String source = item.path("publisher").asText("");
                String url = item.path("link").asText("");
                String date = formatPublishDate(item.path("providerPublishTime"));
                news.add(new NewsItem(title, source, date, url));
                if (news.size() >= limit) {
                    break;
                }
            }
            return news;
        } catch (Exception e) {
            log.warn("Yahoo Finance: échec de récupération des actualités ({})", e.getMessage());
            return List.of();
        }
    }

    private String formatPublishDate(JsonNode publishTimeNode) {
        if (publishTimeNode.isMissingNode() || publishTimeNode.isNull()) {
            return "";
        }
        try {
            return Instant.ofEpochSecond(publishTimeNode.asLong()).toString();
        } catch (Exception e) {
            return "";
        }
    }
}
