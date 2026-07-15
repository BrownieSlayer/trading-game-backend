package app.services.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.configuration.game.MinigameConstants;
import app.dto.minigames.EmploymentDto;
import app.dto.minigames.JobOfferDto;
import app.exceptions.InsufficientFundsException;
import app.models.EmploymentState;
import app.models.JobOffer;
import app.models.Portfolio;
import app.models.User;
import app.repositories.EmploymentStateRepository;
import app.repositories.JobOfferRepository;
import app.repositories.PortfolioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Vérifie l'Emploi porté depuis {@code minigames/emploi.py} : tirage
 * pondéré du palier d'offre selon le niveau de compétences cumulé, crédit
 * du poste à échéance fixe (pas d'accrual continu), coûts exponentiels des
 * compétences.
 */
@ExtendWith(MockitoExtension.class)
class EmploymentServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("UTC"));

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private EmploymentStateRepository employmentStateRepository;
    @Mock
    private JobOfferRepository jobOfferRepository;

    private User user;
    private Portfolio portfolio;

    private EmploymentServiceImpl service() {
        return new EmploymentServiceImpl(portfolioRepository, employmentStateRepository, jobOfferRepository, CLOCK, new Random(7));
    }

    private void givenPortfolio(BigDecimal cash) {
        user = new User();
        user.setId(1L);

        portfolio = new Portfolio();
        portfolio.setId(10L);
        portfolio.setUser(user);
        portfolio.setCash(cash);
    }

    private EmploymentState stateWithSkills(int manual, int intellect, int computer) {
        EmploymentState state = new EmploymentState();
        state.setId(20L);
        state.setPortfolio(portfolio);
        state.setManualLevel(manual);
        state.setIntellectLevel(intellect);
        state.setComputerLevel(computer);
        state.setTotalEarned(BigDecimal.ZERO.setScale(2));
        return state;
    }

    // --- Tirage pondéré du palier (fonction pure) --------------------------

    @Test
    void drawTierAtZeroSkillLevelCoversTheWeightedBucketsByBoundaries() {
        // nul: poids 100, median: poids 5, fou: poids max(0, 0) = 0 -> total 105
        var tiers = MinigameConstants.Emploi.TIERS;
        assertThat(EmploymentServiceImpl.drawTier(tiers, 0, 0)).isEqualTo("nul");
        assertThat(EmploymentServiceImpl.drawTier(tiers, 0, 99)).isEqualTo("nul");
        assertThat(EmploymentServiceImpl.drawTier(tiers, 0, 100)).isEqualTo("median");
        assertThat(EmploymentServiceImpl.drawTier(tiers, 0, 104)).isEqualTo("median");
    }

    @Test
    void drawTierAtMaxSkillLevelMakesTheCrazyTierReachable() {
        // niveau total 45 (3 compétences à 15) : nul=10, median=140, fou=45, total=195
        var tiers = MinigameConstants.Emploi.TIERS;
        assertThat(EmploymentServiceImpl.drawTier(tiers, 45, 9)).isEqualTo("nul");
        assertThat(EmploymentServiceImpl.drawTier(tiers, 45, 10)).isEqualTo("median");
        assertThat(EmploymentServiceImpl.drawTier(tiers, 45, 149)).isEqualTo("median");
        assertThat(EmploymentServiceImpl.drawTier(tiers, 45, 150)).isEqualTo("fou");
        assertThat(EmploymentServiceImpl.drawTier(tiers, 45, 194)).isEqualTo("fou");
    }

    // --- searchOffers() -----------------------------------------------------

    @Test
    void searchingOffersGeneratesThreeOffersAndReplacesThePreviousOnes() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        EmploymentState state = stateWithSkills(0, 0, 0);
        when(employmentStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(state));
        when(jobOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<JobOfferDto> offers = service().searchOffers(user);

        assertThat(offers).hasSize(MinigameConstants.Emploi.OFFER_COUNT);
        verify(jobOfferRepository).deleteByPortfolioId(10L);
    }

    @Test
    void cannotSearchOffersWhileAJobIsActive() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        EmploymentState state = stateWithSkills(0, 0, 0);
        state.setActiveJobTitle("Comptable");
        state.setActiveJobEndsAt(NOW.plusSeconds(3600));
        when(employmentStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(state));

        assertThatThrownBy(() -> service().searchOffers(user)).isInstanceOf(IllegalStateException.class);
        verify(jobOfferRepository, never()).deleteByPortfolioId(any());
    }

    // --- acceptOffer() -------------------------------------------------------

    @Test
    void acceptingAnOfferStartsAnActiveJobAndClearsTheOfferList() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        EmploymentState state = stateWithSkills(0, 0, 0);
        when(employmentStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(state));

        JobOffer offer = new JobOffer();
        offer.setPortfolio(portfolio);
        offer.setTitle("Comptable");
        offer.setTier("median");
        offer.setDurationHours(4);
        offer.setHourlyWage(new BigDecimal("10.00"));
        offer.setTotalWage(new BigDecimal("40.00"));
        when(jobOfferRepository.findByPortfolioIdOrderByIdAsc(10L)).thenReturn(List.of(offer));

        EmploymentDto dto = service().acceptOffer(user, 0);

        assertThat(dto.activeJob()).isNotNull();
        assertThat(dto.activeJob().title()).isEqualTo("Comptable");
        assertThat(dto.activeJob().endsAt()).isEqualTo(NOW.plusSeconds(4 * 3600));
        verify(jobOfferRepository).deleteByPortfolioId(10L);
    }

    @Test
    void acceptingAnInvalidOfferIndexIsRejected() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(employmentStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(stateWithSkills(0, 0, 0)));
        when(jobOfferRepository.findByPortfolioIdOrderByIdAsc(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> service().acceptOffer(user, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotAcceptAnOfferWhileAJobIsAlreadyActive() {
        givenPortfolio(new BigDecimal("10000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        EmploymentState state = stateWithSkills(0, 0, 0);
        state.setActiveJobTitle("Comptable");
        state.setActiveJobEndsAt(NOW.plusSeconds(3600));
        when(employmentStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(state));

        assertThatThrownBy(() -> service().acceptOffer(user, 0)).isInstanceOf(IllegalStateException.class);
    }

    // --- Crédit du poste à échéance -------------------------------------------

    @Test
    void aCompletedJobIsCreditedAndCleared() {
        givenPortfolio(new BigDecimal("1000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        EmploymentState state = stateWithSkills(0, 0, 0);
        state.setActiveJobTitle("Comptable");
        state.setActiveJobTier("median");
        state.setActiveJobDurationHours(4);
        state.setActiveJobStartedAt(NOW.minusSeconds(4 * 3600));
        state.setActiveJobEndsAt(NOW.minusSeconds(1)); // échéance dépassée
        state.setActiveJobHourlyWage(new BigDecimal("10.00"));
        state.setActiveJobTotalWage(new BigDecimal("40.00"));
        when(employmentStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(state));

        EmploymentDto dto = service().getStatus(user);

        assertThat(portfolio.getCash()).isEqualByComparingTo("1040.00");
        assertThat(dto.totalEarned()).isEqualByComparingTo("40.00");
        assertThat(dto.activeJob()).isNull();
        assertThat(state.getActiveJobEndsAt()).isNull();
    }

    @Test
    void aJobNotYetDueIsNotCredited() {
        givenPortfolio(new BigDecimal("1000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        EmploymentState state = stateWithSkills(0, 0, 0);
        state.setActiveJobTitle("Comptable");
        state.setActiveJobTier("median");
        state.setActiveJobDurationHours(4);
        state.setActiveJobStartedAt(NOW);
        state.setActiveJobEndsAt(NOW.plusSeconds(3600));
        state.setActiveJobHourlyWage(new BigDecimal("10.00"));
        state.setActiveJobTotalWage(new BigDecimal("40.00"));
        when(employmentStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(state));

        EmploymentDto dto = service().getStatus(user);

        assertThat(portfolio.getCash()).isEqualByComparingTo("1000.00");
        assertThat(dto.activeJob()).isNotNull();
    }

    // --- upgradeSkill() ----------------------------------------------------

    @Test
    void upgradingASkillIncrementsTheLevelAndDeductsTheCost() {
        givenPortfolio(new BigDecimal("1000.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(employmentStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(stateWithSkills(0, 0, 0)));

        // "manuel" niveau 0 -> coût de base 100 €
        EmploymentDto dto = service().upgradeSkill(user, "manuel");

        assertThat(dto.skills().get("manuel").level()).isEqualTo(1);
        assertThat(portfolio.getCash()).isEqualByComparingTo("900.00");
    }

    @Test
    void upgradingASkillWithInsufficientFundsIsRejected() {
        givenPortfolio(new BigDecimal("10.00"));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(employmentStateRepository.findByPortfolioId(10L)).thenReturn(Optional.of(stateWithSkills(0, 0, 0)));

        assertThatThrownBy(() -> service().upgradeSkill(user, "manuel")).isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void upgradingAnUnknownSkillIsRejected() {
        givenPortfolio(new BigDecimal("10000.00"));

        assertThatThrownBy(() -> service().upgradeSkill(user, "not-a-real-skill")).isInstanceOf(IllegalArgumentException.class);
    }
}
