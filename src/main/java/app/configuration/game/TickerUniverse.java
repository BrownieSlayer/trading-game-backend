package app.configuration.game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Univers de valeurs négociables : CAC 40 + grandes capitalisations US +
 * cryptomonnaies majeures + ETF cotés à Paris. Porté depuis
 * {@code core/config.py:TICKER_GROUPS} du jeu de référence (Bourse Game),
 * tickers validés individuellement via Yahoo Finance/Binance.
 */
public final class TickerUniverse {

    private static final Map<String, String> CAC_40 = Map.ofEntries(
            Map.entry("AI.PA", "Air Liquide"),
            Map.entry("AIR.PA", "Airbus"),
            Map.entry("MT.AS", "ArcelorMittal"),
            Map.entry("CS.PA", "AXA"),
            Map.entry("BNP.PA", "BNP Paribas"),
            Map.entry("EN.PA", "Bouygues"),
            Map.entry("CAP.PA", "Capgemini"),
            Map.entry("CA.PA", "Carrefour"),
            Map.entry("ACA.PA", "Crédit Agricole"),
            Map.entry("BN.PA", "Danone"),
            Map.entry("DSY.PA", "Dassault Systèmes"),
            Map.entry("EDEN.PA", "Edenred"),
            Map.entry("ENGI.PA", "Engie"),
            Map.entry("EL.PA", "EssilorLuxottica"),
            Map.entry("ERF.PA", "Eurofins Scientific"),
            Map.entry("RMS.PA", "Hermès"),
            Map.entry("KER.PA", "Kering"),
            Map.entry("LR.PA", "Legrand"),
            Map.entry("OR.PA", "L'Oréal"),
            Map.entry("MC.PA", "LVMH"),
            Map.entry("ML.PA", "Michelin"),
            Map.entry("ORA.PA", "Orange"),
            Map.entry("RI.PA", "Pernod Ricard"),
            Map.entry("PUB.PA", "Publicis"),
            Map.entry("RNO.PA", "Renault"),
            Map.entry("SAF.PA", "Safran"),
            Map.entry("SGO.PA", "Saint-Gobain"),
            Map.entry("SAN.PA", "Sanofi"),
            Map.entry("SU.PA", "Schneider Electric"),
            Map.entry("GLE.PA", "Société Générale"),
            Map.entry("STLAP.PA", "Stellantis"),
            Map.entry("STMPA.PA", "STMicroelectronics"),
            Map.entry("TEP.PA", "Teleperformance"),
            Map.entry("HO.PA", "Thales"),
            Map.entry("TTE.PA", "TotalEnergies"),
            Map.entry("URW.PA", "Unibail-Rodamco-Westfield"),
            Map.entry("VIE.PA", "Veolia"),
            Map.entry("DG.PA", "Vinci"),
            Map.entry("VIV.PA", "Vivendi"),
            Map.entry("WLN.PA", "Worldline")
    );

    private static final Map<String, String> US = Map.ofEntries(
            Map.entry("AAPL", "Apple"),
            Map.entry("MSFT", "Microsoft"),
            Map.entry("AMZN", "Amazon"),
            Map.entry("GOOGL", "Alphabet (Google)"),
            Map.entry("NVDA", "Nvidia"),
            Map.entry("TSLA", "Tesla"),
            Map.entry("META", "Meta"),
            Map.entry("BRK-B", "Berkshire Hathaway"),
            Map.entry("JPM", "JPMorgan Chase"),
            Map.entry("V", "Visa"),
            Map.entry("UNH", "UnitedHealth"),
            Map.entry("JNJ", "Johnson & Johnson"),
            Map.entry("WMT", "Walmart"),
            Map.entry("PG", "Procter & Gamble"),
            Map.entry("MA", "Mastercard"),
            Map.entry("HD", "Home Depot"),
            Map.entry("CVX", "Chevron"),
            Map.entry("MRK", "Merck"),
            Map.entry("KO", "Coca-Cola"),
            Map.entry("PEP", "PepsiCo"),
            Map.entry("ABBV", "AbbVie"),
            Map.entry("AVGO", "Broadcom"),
            Map.entry("COST", "Costco"),
            Map.entry("DIS", "Walt Disney"),
            Map.entry("MCD", "McDonald's"),
            Map.entry("NFLX", "Netflix"),
            Map.entry("ADBE", "Adobe"),
            Map.entry("CRM", "Salesforce"),
            Map.entry("INTC", "Intel"),
            Map.entry("AMD", "AMD"),
            Map.entry("CSCO", "Cisco"),
            Map.entry("ORCL", "Oracle"),
            Map.entry("PFE", "Pfizer"),
            Map.entry("TMO", "Thermo Fisher"),
            Map.entry("ABT", "Abbott"),
            Map.entry("XOM", "ExxonMobil"),
            Map.entry("BAC", "Bank of America"),
            Map.entry("WFC", "Wells Fargo"),
            Map.entry("NKE", "Nike"),
            Map.entry("LIN", "Linde"),
            Map.entry("TXN", "Texas Instruments"),
            Map.entry("QCOM", "Qualcomm"),
            Map.entry("HON", "Honeywell"),
            Map.entry("UPS", "UPS"),
            Map.entry("IBM", "IBM"),
            Map.entry("GE", "General Electric"),
            Map.entry("CAT", "Caterpillar"),
            Map.entry("BA", "Boeing"),
            Map.entry("GS", "Goldman Sachs"),
            Map.entry("AXP", "American Express")
    );

    private static final Map<String, String> CRYPTO = Map.ofEntries(
            Map.entry("BTC-USD", "Bitcoin"),
            Map.entry("ETH-USD", "Ethereum"),
            Map.entry("BNB-USD", "BNB"),
            Map.entry("SOL-USD", "Solana"),
            Map.entry("XRP-USD", "XRP"),
            Map.entry("ADA-USD", "Cardano"),
            Map.entry("DOGE-USD", "Dogecoin"),
            Map.entry("AVAX-USD", "Avalanche"),
            Map.entry("DOT-USD", "Polkadot"),
            Map.entry("LINK-USD", "Chainlink"),
            Map.entry("LTC-USD", "Litecoin"),
            Map.entry("BCH-USD", "Bitcoin Cash"),
            Map.entry("SHIB-USD", "Shiba Inu"),
            Map.entry("TRX-USD", "Tron"),
            Map.entry("ATOM-USD", "Cosmos")
    );

    // ETF cotés à Paris (déjà en euros, aucune conversion de devise nécessaire).
    private static final Map<String, String> ETF = Map.ofEntries(
            Map.entry("CW8.PA", "Amundi MSCI World"),
            Map.entry("PE500.PA", "Amundi PEA S&P 500"),
            Map.entry("PANX.PA", "Amundi Nasdaq-100"),
            Map.entry("C40.PA", "Lyxor CAC 40"),
            Map.entry("MEUD.PA", "Lyxor Stoxx Europe 600"),
            Map.entry("PAEEM.PA", "Amundi MSCI Emerging Markets"),
            Map.entry("PAASI.PA", "Amundi MSCI EM Asia"),
            Map.entry("ETZ.PA", "Amundi Physical Gold"),
            Map.entry("C3M.PA", "Amundi Euro Corporate Bond"),
            Map.entry("GOVY.PA", "Amundi Global Govies")
    );

    /** {@link TickerGroup} -> {ticker -> nom}. Ne pas modifier directement les maps imbriquées (immuables). */
    public static final Map<TickerGroup, Map<String, String>> TICKER_GROUPS = Map.of(
            TickerGroup.CAC_40, CAC_40,
            TickerGroup.US, US,
            TickerGroup.CRYPTO, CRYPTO,
            TickerGroup.ETF, ETF
    );

    /** Vue plate {ticker -> nom}, dérivée de {@link #TICKER_GROUPS}. */
    public static final Map<String, String> TICKER_BASKET = flatten();

    /** Vue plate {ticker -> groupe}, dérivée de {@link #TICKER_GROUPS}. */
    public static final Map<String, TickerGroup> TICKER_GROUP_BY_TICKER = flattenGroups();

    private TickerUniverse() {}

    private static Map<String, String> flatten() {
        Map<String, String> result = new LinkedHashMap<>();
        TICKER_GROUPS.forEach((group, tickers) -> result.putAll(tickers));
        return Map.copyOf(result);
    }

    private static Map<String, TickerGroup> flattenGroups() {
        Map<String, TickerGroup> result = new LinkedHashMap<>();
        TICKER_GROUPS.forEach((group, tickers) -> tickers.keySet().forEach(ticker -> result.put(ticker, group)));
        return Map.copyOf(result);
    }

    /** Groupe auquel appartient ce ticker, vide si inconnu de l'univers. */
    public static Optional<TickerGroup> groupOf(String ticker) {
        return Optional.ofNullable(TICKER_GROUP_BY_TICKER.get(ticker));
    }

    /** Nom lisible de ce ticker, ou {@code null} si inconnu de l'univers. */
    public static String nameOf(String ticker) {
        return TICKER_BASKET.get(ticker);
    }

    public static boolean isKnown(String ticker) {
        return TICKER_BASKET.containsKey(ticker);
    }
}
