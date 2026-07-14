package app.configuration.game;

/**
 * Amélioration/compétence à niveaux et coût exponentiel : le coût du niveau
 * N (0-indexé, niveau actuel avant achat) vaut {@code baseCost *
 * costMultiplier^N}. Forme partagée par les améliorations Business et les
 * compétences Emploi.
 */
public record LeveledUpgrade(String label, double baseCost, double costMultiplier, double effectPerLevel, int maxLevel) {

    public double costForLevel(int currentLevel) {
        return baseCost * Math.pow(costMultiplier, currentLevel);
    }
}
