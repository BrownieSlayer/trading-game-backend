package app.dto.market;

/** Horaires d'ouverture/fermeture (heure locale de la machine) d'une place boursière, et si elle est actuellement ouverte. */
public record MarketStatus(String name, String openLocal, String closeLocal, boolean open) {}
