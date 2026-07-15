package app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.dto.minigames.ReflexActionRequest;
import app.dto.minigames.TradingRapideStatusDto;
import app.services.CurrentUserResolver;
import app.services.TradingRapideService;
import jakarta.validation.Valid;

/**
 * Mini-jeu "Trading rapide" — marche aléatoire générée côté serveur, le
 * client ne fait que rejouer l'animation et envoyer ses actions (achat/
 * vente/abandon), toujours revalidées contre le tick réellement écoulé.
 */
@RestController
@RequestMapping("/api/minigames/trading-rapide")
public class TradingRapideController {

    private final TradingRapideService tradingRapideService;
    private final CurrentUserResolver currentUserResolver;

    public TradingRapideController(TradingRapideService tradingRapideService, CurrentUserResolver currentUserResolver) {
        this.tradingRapideService = tradingRapideService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/start")
    public ResponseEntity<TradingRapideStatusDto> start(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(tradingRapideService.start(currentUserResolver.resolve(userDetails)));
    }

    @PostMapping("/action")
    public ResponseEntity<TradingRapideStatusDto> act(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody ReflexActionRequest request
    ) {
        TradingRapideStatusDto status = tradingRapideService.act(currentUserResolver.resolve(userDetails), request.action());
        return ResponseEntity.ok(status);
    }

    @GetMapping
    public ResponseEntity<TradingRapideStatusDto> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(tradingRapideService.getStatus(currentUserResolver.resolve(userDetails)));
    }
}
