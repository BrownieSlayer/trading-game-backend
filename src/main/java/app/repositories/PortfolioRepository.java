package app.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.models.Portfolio;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByUserId(Long userId);
}
