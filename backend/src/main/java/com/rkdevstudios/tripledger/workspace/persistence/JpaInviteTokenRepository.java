package com.rkdevstudios.tripledger.workspace.persistence;

import com.rkdevstudios.tripledger.workspace.domain.InviteToken;
import com.rkdevstudios.tripledger.workspace.domain.InviteTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaInviteTokenRepository extends JpaRepository<InviteToken, String>, InviteTokenRepository {
}
