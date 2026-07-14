package app.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import app.models.PortfolioValueSnapshot;

public interface PortfolioValueSnapshotRepository extends JpaRepository<PortfolioValueSnapshot, Long> {

    boolean existsByPortfolioIdAndSnapshotDate(Long portfolioId, LocalDate snapshotDate);

    List<PortfolioValueSnapshot> findByPortfolioIdOrderBySnapshotDateAsc(Long portfolioId);
}
