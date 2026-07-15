package app.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.enums.BetStatus;
import app.models.Bet;

public interface BetRepository extends JpaRepository<Bet, Long> {

    Optional<Bet> findByPortfolioIdAndStatus(Long portfolioId, BetStatus status);
}
