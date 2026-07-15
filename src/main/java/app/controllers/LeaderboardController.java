package app.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.dto.leaderboard.LeaderboardEntryDto;
import app.services.LeaderboardService;

/** Classement global des comptes par performance — visible par tout utilisateur authentifié (pas seulement le sien). */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(limit));
    }
}
