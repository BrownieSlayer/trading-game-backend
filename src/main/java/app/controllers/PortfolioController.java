package app.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import app.dto.portfolio.BuyRequest;
import app.dto.portfolio.PortfolioDto;
import app.dto.portfolio.PortfolioValuePointDto;
import app.dto.portfolio.SellRequest;
import app.dto.portfolio.TransactionDto;
import app.services.CurrentUserResolver;
import app.services.PortfolioService;
import jakarta.validation.Valid;

/**
 * Portefeuille de l'utilisateur connecté : valorisation, achat, vente,
 * historique des transactions et de la valeur. {@link CurrentUserResolver}
 * résout paresseusement le "nouveau jour" (streak, snapshot de valeur,
 * résolution du pari en cours) avant chaque requête.
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final CurrentUserResolver currentUserResolver;

    public PortfolioController(PortfolioService portfolioService, CurrentUserResolver currentUserResolver) {
        this.portfolioService = portfolioService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public ResponseEntity<PortfolioDto> getPortfolio(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(portfolioService.getPortfolio(currentUserResolver.resolve(userDetails)));
    }

    @PostMapping("/buy")
    public ResponseEntity<TransactionDto> buy(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody BuyRequest request
    ) {
        TransactionDto transaction = portfolioService.buy(currentUserResolver.resolve(userDetails), request.ticker(), request.quantity());
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/sell")
    public ResponseEntity<TransactionDto> sell(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody SellRequest request
    ) {
        TransactionDto transaction = portfolioService.sell(currentUserResolver.resolve(userDetails), request.ticker(), request.quantity());
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDto>> getTransactions(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(portfolioService.getTransactions(currentUserResolver.resolve(userDetails), limit));
    }

    /** Historique de la valeur du portefeuille, un point par jour joué. */
    @GetMapping("/value-history")
    public ResponseEntity<List<PortfolioValuePointDto>> getValueHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(portfolioService.getValueHistory(currentUserResolver.resolve(userDetails)));
    }
}
