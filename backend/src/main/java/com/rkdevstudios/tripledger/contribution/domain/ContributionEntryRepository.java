package com.rkdevstudios.tripledger.contribution.domain;

import java.util.List;

public interface ContributionEntryRepository {
    ContributionEntry save(ContributionEntry entry);
    List<ContributionEntry> findByWorkspaceIdAndUserId(String workspaceId, String userId);
    List<ContributionEntry> findByWorkspaceId(String workspaceId);
}
