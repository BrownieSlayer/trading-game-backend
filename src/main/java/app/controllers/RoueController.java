package app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.dto.minigames.WheelSpinDto;
import app.services.CurrentUserResolver;
import app.services.RoueService;

/** Mini-jeu "Roue de la fortune" — un tirage pondéré par jour civil, verrou quotidien indépendant du reste. */
@RestController
@RequestMapping("/api/minigames/roue")
public class RoueController {

    private final RoueService roueService;
    private final CurrentUserResolver currentUserResolver;

    public RoueController(RoueService roueService, CurrentUserResolver currentUserResolver) {
        this.roueService = roueService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/spin")
    public ResponseEntity<WheelSpinDto> spin(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(roueService.spin(currentUserResolver.resolve(userDetails)));
    }

    /** Le résultat du tirage du jour, ou 204 si pas encore joué aujourd'hui. */
    @GetMapping
    public ResponseEntity<WheelSpinDto> getTodayResult(@AuthenticationPrincipal UserDetails userDetails) {
        return roueService.getTodayResult(currentUserResolver.resolve(userDetails))
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
