package app.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import app.models.JobOffer;

public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    List<JobOffer> findByPortfolioIdOrderByIdAsc(Long portfolioId);

    void deleteByPortfolioId(Long portfolioId);
}
