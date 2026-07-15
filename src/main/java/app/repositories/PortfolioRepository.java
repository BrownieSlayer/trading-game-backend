package app.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import app.models.Portfolio;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByUserId(Long userId);

    /** Tous les portefeuilles avec leur utilisateur chargé en une requête (évite le N+1 sur le username, ex. classement). */
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.user")
    List<Portfolio> findAllWithUser();
}
