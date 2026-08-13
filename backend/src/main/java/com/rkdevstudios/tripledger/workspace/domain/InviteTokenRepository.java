package com.rkdevstudios.tripledger.workspace.domain;

import java.util.Optional;

public interface InviteTokenRepository {
    InviteToken save(InviteToken inviteToken);
    Optional<InviteToken> findByToken(String token);
    void deleteById(String token);
}
