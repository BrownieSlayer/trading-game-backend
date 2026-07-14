package app.services.market;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

import app.dto.market.PriceQuote;

/**
 * Client pour l'API publique Binance (gratuite, sans clé) : source des
 * cours crypto en quasi temps réel, le flux crypto de Yahoo Finance étant
 * régulièrement figé pendant des heures (constat identique côté jeu Python
 * de référence — voir {@code core/market.py}).
 */
@Component
public class BinanceClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceClient.class);
    private static final String TICKER_24H_URL = "https://api.binance.com/api/v3/ticker/24hr";

    private final RestClient restClient;

    public BinanceClient(RestClient marketDataRestClient) {
        this.restClient = marketDataRestClient;
    }

    /** Prix + variation 24h (en USD) pour un ensemble de tickers crypto au format du jeu (ex. "BTC-USD"). */
    public Map<String, PriceQuote> fetchQuotes(Set<String> tickers) {
        if (tickers.isEmpty()) {
            return Map.of();
        }

        Map<String, String> tickerByBinanceSymbol = new LinkedHashMap<>();
        for (String ticker : tickers) {
            tickerByBinanceSymbol.put(toBinanceSymbol(ticker), ticker);
        }

        String symbolsParam = tickerByBinanceSymbol.keySet().stream()
            .collect(Collectors.joining("\",\"", "[\"", "\"]"));

        try {
            JsonNode response = restClient.get()
                .uri(TICKER_24H_URL + "?symbols={symbols}", symbolsParam)
                .retrieve()
                .body(JsonNode.class);

            if (response == null || !response.isArray()) {
                return Map.of();
            }

            Map<String, PriceQuote> result = new LinkedHashMap<>();
            for (JsonNode entry : response) {
                String symbol = entry.path("symbol").asText(null);
                String ticker = tickerByBinanceSymbol.get(symbol);
                if (ticker == null) {
                    continue;
                }
                try {
                    double price = Double.parseDouble(entry.path("lastPrice").asText());
                    double changePercent = Double.parseDouble(entry.path("priceChangePercent").asText());
                    result.put(ticker, new PriceQuote(ticker, price, changePercent));
                } catch (NumberFormatException e) {
                    log.warn("Binance: réponse invalide pour {}", symbol);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Binance: échec de récupération des cours crypto ({})", e.getMessage());
            return Map.of();
        }
    }

    private String toBinanceSymbol(String ticker) {
        return ticker.endsWith("-USD") ? ticker.substring(0, ticker.length() - 4) + "USDT" : ticker;
    }
}
