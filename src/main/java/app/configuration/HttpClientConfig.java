package app.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Client HTTP partagé pour les appels sortants vers les API de données de
 * marché (Yahoo Finance, Binance) — voir {@code app.services.market}.
 * Un User-Agent de navigateur est nécessaire : Yahoo rejette les requêtes
 * dépourvues de User-Agent ou envoyant celui par défaut du JDK.
 */
@Configuration
public class HttpClientConfig {

    private static final String BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    @Bean
    public RestClient marketDataRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(10_000);

        return RestClient.builder()
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT)
            .build();
    }
}
