package app.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.models.EmploymentState;

public interface EmploymentStateRepository extends JpaRepository<EmploymentState, Long> {

    Optional<EmploymentState> findByPortfolioId(Long portfolioId);
}
