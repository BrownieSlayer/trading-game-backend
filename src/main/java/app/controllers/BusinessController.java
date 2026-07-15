package app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.dto.minigames.AmountRequest;
import app.dto.minigames.BusinessDto;
import app.services.BusinessService;
import app.services.CurrentUserResolver;
import jakarta.validation.Valid;

/** Mini-jeu "Business" — revenu passif crédité heure par heure, récolte hors-ligne plafonnée. */
@RestController
@RequestMapping("/api/minigames/business")
public class BusinessController {

    private final BusinessService businessService;
    private final CurrentUserResolver currentUserResolver;

    public BusinessController(BusinessService businessService, CurrentUserResolver currentUserResolver) {
        this.businessService = businessService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public ResponseEntity<BusinessDto> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(businessService.getStatus(currentUserResolver.resolve(userDetails)));
    }

    @PostMapping("/invest")
    public ResponseEntity<BusinessDto> invest(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody AmountRequest request
    ) {
        return ResponseEntity.ok(businessService.invest(currentUserResolver.resolve(userDetails), request.amount()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<BusinessDto> withdraw(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody AmountRequest request
    ) {
        return ResponseEntity.ok(businessService.withdraw(currentUserResolver.resolve(userDetails), request.amount()));
    }

    @PostMapping("/upgrade/{type}")
    public ResponseEntity<BusinessDto> upgrade(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable String type
    ) {
        return ResponseEntity.ok(businessService.upgrade(currentUserResolver.resolve(userDetails), type));
    }
}
