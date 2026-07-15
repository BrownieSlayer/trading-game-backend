package app.services;

import app.dto.minigames.TradingRapideStatusDto;
import app.enums.ReflexAction;
import app.models.User;

/**
 * Port de {@code minigames/trading_rapide.py} : marche aléatoire de 25
 * ticks générée et figée côté serveur, jamais par le client. Limite de 3
 * essais/jour, verrou quotidien indépendant (comme Pari/Roue).
 */
public interface TradingRapideService {

    /** Démarre une nouvelle partie. Résout d'abord silencieusement toute partie expirée en attente. */
    TradingRapideStatusDto start(User user);

    TradingRapideStatusDto act(User user, ReflexAction action);

    /** État courant (partie en cours le cas échéant, essais restants aujourd'hui). */
    TradingRapideStatusDto getStatus(User user);
}
