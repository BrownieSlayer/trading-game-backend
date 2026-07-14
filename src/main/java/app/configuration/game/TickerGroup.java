package app.configuration.game;

/**
 * Place boursière/catégorie d'actif d'un ticker. Le label court est celui
 * affiché à côté de chaque valeur dans l'UI (FR/US/CR/ETF).
 */
public enum TickerGroup {
    CAC_40("FR"),
    US("US"),
    CRYPTO("CR"),
    ETF("ETF");

    private final String label;

    TickerGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
