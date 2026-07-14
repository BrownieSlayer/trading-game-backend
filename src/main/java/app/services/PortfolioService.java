package app.services;

import java.math.BigDecimal;
import java.util.List;

import app.dto.portfolio.PortfolioDto;
import app.dto.portfolio.PortfolioValuePointDto;
import app.dto.portfolio.TransactionDto;
import app.models.Portfolio;
import app.models.User;

/** Portage de {@code core/portfolio.py} du jeu de référence (Bourse Game) : achat/vente, valorisation, fiscalité PFU. */
public interface PortfolioService {

    /** Crée le portefeuille initial d'un utilisateur qui vient de s'inscrire (capital de départ, aucune position). */
    Portfolio createPortfolioForUser(User user);

    /** Valorisation courante du portefeuille : liquidités, positions, gain global — les prix viennent exclusivement du cache serveur. */
    PortfolioDto getPortfolio(User user);

    TransactionDto buy(User user, String ticker, BigDecimal quantity);

    TransactionDto sell(User user, String ticker, BigDecimal quantity);

    /** Historique des transactions, les plus récentes en premier. */
    List<TransactionDto> getTransactions(User user, int limit);

    /** Valeur totale courante (liquidités + positions valorisées au cache de prix). Réutilisé par {@code DailyCycleService} pour le snapshot quotidien. */
    BigDecimal getTotalValue(Portfolio portfolio);

    /** Historique de la valeur du portefeuille, un point par jour joué, du plus ancien au plus récent. */
    List<PortfolioValuePointDto> getValueHistory(User user);
}
