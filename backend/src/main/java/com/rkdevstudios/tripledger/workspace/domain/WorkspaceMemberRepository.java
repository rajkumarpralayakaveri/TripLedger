package com.rkdevstudios.tripledger.workspace.domain;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository {
    WorkspaceMember save(WorkspaceMember member);
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(String workspaceId, String userId);
    List<WorkspaceMember> findByWorkspaceId(String workspaceId);
    List<WorkspaceMember> findByUserId(String userId);
    void delete(WorkspaceMember member);
    void deleteByWorkspaceIdAndUserId(String workspaceId, String userId);
}
