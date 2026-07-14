package app.dto.market;

/** Dernier cours connu d'un ticker (toujours en euros) et sa variation % vs la clôture précédente. */
public record PriceQuote(String ticker, double price, double changePercent) {}
