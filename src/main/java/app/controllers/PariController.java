package app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.dto.minigames.BetDto;
import app.dto.minigames.PlaceBetRequest;
import app.services.CurrentUserResolver;
import app.services.PariService;
import jakarta.validation.Valid;

/** Mini-jeu "Pari du jour" — un seul pari actif à la fois, résolu automatiquement à la session suivante. */
@RestController
@RequestMapping("/api/minigames/pari")
public class PariController {

    private final PariService pariService;
    private final CurrentUserResolver currentUserResolver;

    public PariController(PariService pariService, CurrentUserResolver currentUserResolver) {
        this.pariService = pariService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    public ResponseEntity<BetDto> placeBet(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody PlaceBetRequest request
    ) {
        BetDto bet = pariService.placeBet(
            currentUserResolver.resolve(userDetails), request.ticker(), request.direction(), request.stake()
        );
        return ResponseEntity.ok(bet);
    }

    /** Le pari actif, ou 204 s'il n'y en a aucun. */
    @GetMapping
    public ResponseEntity<BetDto> getActiveBet(@AuthenticationPrincipal UserDetails userDetails) {
        return pariService.getActiveBet(currentUserResolver.resolve(userDetails))
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
