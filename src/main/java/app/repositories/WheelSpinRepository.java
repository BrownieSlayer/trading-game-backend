package app.repositories;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.models.WheelSpin;

public interface WheelSpinRepository extends JpaRepository<WheelSpin, Long> {

    boolean existsByPortfolioIdAndSpinDate(Long portfolioId, LocalDate spinDate);

    Optional<WheelSpin> findByPortfolioIdAndSpinDate(Long portfolioId, LocalDate spinDate);
}
