package com.rkdevstudios.tripledger.contribution.domain;

import java.util.List;
import java.util.Optional;

public interface PlannedContributionRepository {
    PlannedContribution save(PlannedContribution plannedContribution);
    Optional<PlannedContribution> findByWorkspaceIdAndUserId(String workspaceId, String userId);
    List<PlannedContribution> findByWorkspaceId(String workspaceId);
    void deleteByWorkspaceIdAndUserId(String workspaceId, String userId);
}
