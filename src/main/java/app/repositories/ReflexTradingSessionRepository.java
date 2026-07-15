package app.repositories;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.enums.ReflexSessionStatus;
import app.models.ReflexTradingSession;

public interface ReflexTradingSessionRepository extends JpaRepository<ReflexTradingSession, Long> {

    Optional<ReflexTradingSession> findByPortfolioIdAndStatus(Long portfolioId, ReflexSessionStatus status);

    long countByPortfolioIdAndSessionDateAndStatus(Long portfolioId, LocalDate sessionDate, ReflexSessionStatus status);
}
