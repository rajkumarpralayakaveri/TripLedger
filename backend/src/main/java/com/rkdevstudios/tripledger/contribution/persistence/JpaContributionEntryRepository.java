package com.rkdevstudios.tripledger.contribution.persistence;

import com.rkdevstudios.tripledger.contribution.domain.ContributionEntry;
import com.rkdevstudios.tripledger.contribution.domain.ContributionEntryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaContributionEntryRepository extends JpaRepository<ContributionEntry, String>, ContributionEntryRepository {
}
