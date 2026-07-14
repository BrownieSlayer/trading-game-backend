package app.configuration.game;

/**
 * Constantes générales du jeu, portées depuis {@code core/config.py} du jeu
 * de référence (Bourse Game).
 */
public final class GameConstants {

    private GameConstants() {}

    /** Capital de départ par défaut d'un nouveau portefeuille. */
    public static final double STARTING_CAPITAL = 10_000.0;

    /**
     * Prélèvement Forfaitaire Unique (PFU / "flat tax") sur les plus-values
     * de cession, régime compte-titres ordinaire (pas de PEA modélisé).
     * S'applique uniquement sur la plus-value réalisée à la vente, jamais
     * sur le montant total vendu, jamais en cas de moins-value.
     */
    public static final double CAPITAL_GAINS_TAX_RATE = 0.30;
}
