package app.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.dto.minigames.EmploymentDto;
import app.dto.minigames.JobOfferDto;
import app.services.CurrentUserResolver;
import app.services.EmploymentService;

/** Mini-jeu "Emploi" — offres à choisir, poste actif crédité une seule fois à échéance fixe. */
@RestController
@RequestMapping("/api/minigames/emploi")
public class EmploymentController {

    private final EmploymentService employmentService;
    private final CurrentUserResolver currentUserResolver;

    public EmploymentController(EmploymentService employmentService, CurrentUserResolver currentUserResolver) {
        this.employmentService = employmentService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public ResponseEntity<EmploymentDto> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(employmentService.getStatus(currentUserResolver.resolve(userDetails)));
    }

    @PostMapping("/offers")
    public ResponseEntity<List<JobOfferDto>> searchOffers(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(employmentService.searchOffers(currentUserResolver.resolve(userDetails)));
    }

    @PostMapping("/accept/{index}")
    public ResponseEntity<EmploymentDto> acceptOffer(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable int index
    ) {
        return ResponseEntity.ok(employmentService.acceptOffer(currentUserResolver.resolve(userDetails), index));
    }

    @PostMapping("/upgrade/{skill}")
    public ResponseEntity<EmploymentDto> upgradeSkill(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable String skill
    ) {
        return ResponseEntity.ok(employmentService.upgradeSkill(currentUserResolver.resolve(userDetails), skill));
    }
}
