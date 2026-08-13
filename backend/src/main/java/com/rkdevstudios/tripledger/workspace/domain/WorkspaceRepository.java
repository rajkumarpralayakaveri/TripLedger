package com.rkdevstudios.tripledger.workspace.domain;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository {
    Workspace save(Workspace workspace);
    Optional<Workspace> findById(String id);
    List<Workspace> findAll();
    void deleteById(String id);
}
