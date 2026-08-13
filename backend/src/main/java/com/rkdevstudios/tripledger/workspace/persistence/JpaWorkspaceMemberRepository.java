package com.rkdevstudios.tripledger.workspace.persistence;

import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMember;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberId;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaWorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId>, WorkspaceMemberRepository {
}
