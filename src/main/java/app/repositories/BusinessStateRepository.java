package app.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.models.BusinessState;

public interface BusinessStateRepository extends JpaRepository<BusinessState, Long> {

    Optional<BusinessState> findByPortfolioId(Long portfolioId);
}
