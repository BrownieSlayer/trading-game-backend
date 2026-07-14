package app.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import app.dto.portfolio.BuyRequest;
import app.dto.portfolio.PortfolioDto;
import app.dto.portfolio.PortfolioValuePointDto;
import app.dto.portfolio.SellRequest;
import app.dto.portfolio.TransactionDto;
import app.models.User;
import app.repositories.UserRepository;
import app.services.DailyCycleService;
import app.services.PortfolioService;
import jakarta.validation.Valid;

/**
 * Portefeuille de l'utilisateur connecté : valorisation, achat, vente,
 * historique des transactions et de la valeur. Chaque appel à
 * {@link #currentUser} résout paresseusement le "nouveau jour" (streak,
 * snapshot de valeur — voir {@link DailyCycleService}) avant de servir la
 * requête, pour que ce soit fait dès le premier accès authentifié de la
 * journée à n'importe quelle route du portefeuille.
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final DailyCycleService dailyCycleService;
    private final UserRepository userRepository;

    public PortfolioController(PortfolioService portfolioService, DailyCycleService dailyCycleService, UserRepository userRepository) {
        this.portfolioService = portfolioService;
        this.dailyCycleService = dailyCycleService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<PortfolioDto> getPortfolio(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(portfolioService.getPortfolio(currentUser(userDetails)));
    }

    @PostMapping("/buy")
    public ResponseEntity<TransactionDto> buy(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody BuyRequest request
    ) {
        TransactionDto transaction = portfolioService.buy(currentUser(userDetails), request.ticker(), request.quantity());
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/sell")
    public ResponseEntity<TransactionDto> sell(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody SellRequest request
    ) {
        TransactionDto transaction = portfolioService.sell(currentUser(userDetails), request.ticker(), request.quantity());
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDto>> getTransactions(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(portfolioService.getTransactions(currentUser(userDetails), limit));
    }

    /** Historique de la valeur du portefeuille, un point par jour joué. */
    @GetMapping("/value-history")
    public ResponseEntity<List<PortfolioValuePointDto>> getValueHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(portfolioService.getValueHistory(currentUser(userDetails)));
    }

    private User currentUser(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        dailyCycleService.resolveNewDay(user);
        return user;
    }
}
