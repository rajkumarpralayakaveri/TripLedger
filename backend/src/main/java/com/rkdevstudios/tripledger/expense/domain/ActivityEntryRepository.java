package com.rkdevstudios.tripledger.expense.domain;

import java.util.List;

public interface ActivityEntryRepository {
    ActivityEntry save(ActivityEntry entry);
    List<ActivityEntry> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
}
