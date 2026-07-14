package app.configuration.game;

/**
 * Palier de salaire d'une offre d'emploi. Le poids de tirage d'une offre
 * dans ce palier vaut {@code max(0, baseWeight + weightPerSkillLevel * niveauTotalCompetences)}.
 */
public record EmploiTier(double minHourlyWage, double maxHourlyWage, double baseWeight, double weightPerSkillLevel) {}
