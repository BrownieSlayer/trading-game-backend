package app.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.models.Holding;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    List<Holding> findByPortfolioId(Long portfolioId);

    Optional<Holding> findByPortfolioIdAndTicker(Long portfolioId, String ticker);
}
