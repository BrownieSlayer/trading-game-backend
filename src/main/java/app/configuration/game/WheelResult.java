package app.configuration.game;

/** Un résultat possible de la roue de la fortune, avec son poids de tirage (pas besoin de sommer à 100). */
public record WheelResult(String label, double gain, int weight) {}
