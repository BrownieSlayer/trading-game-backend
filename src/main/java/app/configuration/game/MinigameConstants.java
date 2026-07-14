package app.configuration.game;

import java.util.List;
import java.util.Map;

/**
 * Valeurs réglables des mini-jeux, portées depuis {@code minigames/config.py}
 * du jeu de référence (Bourse Game). Ne couvre que les paramètres de
 * gameplay (formules, probabilités, bornes) — les catalogues de contenu
 * (90 métiers/icônes pour Emploi, 100 événements narratifs) sont du
 * contenu à part entière, seedé en base au moment où ces mini-jeux sont
 * implémentés plutôt que codé en dur ici.
 */
public final class MinigameConstants {

    private MinigameConstants() {}

    public static final class Pari {
        private Pari() {}

        public static final double DEFAULT_STAKE = 50.0;
        /** Gain = mise * ratio si gagné ; mise perdue si raté. */
        public static final double GAIN_RATIO = 0.8;
    }

    public static final class Roue {
        private Roue() {}

        public static final List<WheelResult> RESULTS = List.of(
                new WheelResult("Rien ce coup-ci", 0.0, 40),
                new WheelResult("Petit bonus", 10.0, 35),
                new WheelResult("Bon bonus", 30.0, 15),
                new WheelResult("Gros lot", 100.0, 8),
                new WheelResult("JACKPOT", 500.0, 2)
        );
    }

    public static final class TradingRapide {
        private TradingRapide() {}

        public static final double TICK_INTERVAL_SECONDS = 0.75;
        public static final int TICK_COUNT = 25;
        public static final double DURATION_SECONDS = TICK_COUNT * TICK_INTERVAL_SECONDS;
        /** Base notionnelle sur laquelle le % de variation de chaque tick est appliqué. */
        public static final double NOTIONAL_STAKE = 200.0;
        /** Écart-type du pas de la marche aléatoire (par tick). */
        public static final double VOLATILITY = 0.02;
        public static final int DAILY_ATTEMPTS = 3;
    }

    public static final class Business {
        private Business() {}

        /** Taux horaire de base (avant amélioration "rendement"), ex. 0.005 = 0.5 %/h. */
        public static final double HOURLY_RATE = 0.005;
        public static final double COEFFICIENT_THRESHOLD = 5000.0;
        public static final double MIN_COEFFICIENT = 0.20;
        /** Aléa ±10 %, appliqué indépendamment à chaque heure récoltée. */
        public static final double RANDOM_VARIATION = 0.10;
        public static final int BASE_OFFLINE_HOURS_LIMIT = 24;
        public static final double BASE_MAX_INVESTMENT = 20_000.0;

        public static final Map<String, LeveledUpgrade> UPGRADES = Map.of(
                "capacite", new LeveledUpgrade("Capacité maximale", 500.0, 1.6, 5000.0, 10),
                "rendement", new LeveledUpgrade("Rendement horaire", 800.0, 1.7, 0.0005, 10),
                "stockage", new LeveledUpgrade("Limite hors-ligne", 400.0, 1.5, 12, 8)
        );

        public static double coefficientRendement(double investment) {
            return Math.max(MIN_COEFFICIENT, 1 / (1 + investment / COEFFICIENT_THRESHOLD));
        }
    }

    public static final class Emploi {
        private Emploi() {}

        public static final List<Integer> OFFER_DURATIONS_HOURS = List.of(1, 2, 4, 8);
        public static final int OFFER_COUNT = 3;
        /** Aléa ±15 % appliqué au salaire horaire de chaque offre générée. */
        public static final double WAGE_RANDOM_VARIATION = 0.15;

        public static final Map<String, LeveledUpgrade> SKILLS = Map.of(
                "manuel", new LeveledUpgrade("Habile de ses mains", 100.0, 1.35, 0.8, 15),
                "intellect", new LeveledUpgrade("Gros cerveau", 120.0, 1.35, 1.0, 15),
                "informatique", new LeveledUpgrade("Informaticien", 150.0, 1.4, 1.3, 15)
        );

        public static final Map<String, EmploiTier> TIERS = Map.of(
                "nul", new EmploiTier(1.0, 3.0, 100, -2),
                "median", new EmploiTier(6.0, 15.0, 5, 3),
                "fou", new EmploiTier(40.0, 120.0, 0, 1)
        );
    }

    public static final class Evenements {
        private Evenements() {}

        /** Chance qu'un 2e événement soit tiré le même jour. */
        public static final double SECOND_EVENT_PROBABILITY = 0.30;
        /** Plage horaire (heure locale) dans laquelle un événement peut se déclencher. */
        public static final int MIN_HOUR = 8;
        public static final int MAX_HOUR = 22;
        /** Écart minimal entre les heures de déclenchement s'il y a 2 événements. */
        public static final int MIN_HOUR_GAP = 2;
        /** Probabilité qu'une occurrence tirée soit obligatoire (pas de "Ne rien faire"). */
        public static final double MANDATORY_PROBABILITY = 0.25;

        public static final Map<String, Integer> WEIGHT_BY_RARITY = Map.of(
                "commun", 10,
                "rare", 3,
                "legendaire", 1
        );

        public static final Map<String, AmountRange> AMOUNT_BY_RARITY = Map.of(
                "commun", new AmountRange(15.0, 90.0),
                "rare", new AmountRange(100.0, 350.0),
                "legendaire_gain", new AmountRange(700.0, 2500.0),
                "legendaire_perte", new AmountRange(400.0, 900.0)
        );
    }

    public static final class Dette {
        private Dette() {}

        /** Mensualité théorique = montant_initial / DURATION_DAYS. */
        public static final int DURATION_DAYS = 30;
        /** Heure locale à partir de laquelle le prélèvement du soir a lieu. */
        public static final int EVENING_DEDUCTION_HOUR = 21;
    }
}
