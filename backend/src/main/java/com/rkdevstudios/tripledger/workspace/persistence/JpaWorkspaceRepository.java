package com.rkdevstudios.tripledger.workspace.persistence;

import com.rkdevstudios.tripledger.workspace.domain.Workspace;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaWorkspaceRepository extends JpaRepository<Workspace, String>, WorkspaceRepository {
}
