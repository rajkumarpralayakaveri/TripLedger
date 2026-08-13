package com.rkdevstudios.tripledger.expense.persistence;

import com.rkdevstudios.tripledger.expense.domain.ActivityEntry;
import com.rkdevstudios.tripledger.expense.domain.ActivityEntryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaActivityEntryRepository extends JpaRepository<ActivityEntry, String>, ActivityEntryRepository {
    List<ActivityEntry> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
}
