package app.services;

import java.util.List;

import app.dto.leaderboard.LeaderboardEntryDto;

/** Classement global des comptes par performance (% de gain depuis le capital de départ) — décision produit : visible par tout compte authentifié. */
public interface LeaderboardService {

    List<LeaderboardEntryDto> getLeaderboard(int limit);
}
