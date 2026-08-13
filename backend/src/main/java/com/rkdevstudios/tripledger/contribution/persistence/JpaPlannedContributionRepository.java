package com.rkdevstudios.tripledger.contribution.persistence;

import com.rkdevstudios.tripledger.contribution.domain.PlannedContribution;
import com.rkdevstudios.tripledger.contribution.domain.PlannedContributionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPlannedContributionRepository extends JpaRepository<PlannedContribution, String>, PlannedContributionRepository {
}
