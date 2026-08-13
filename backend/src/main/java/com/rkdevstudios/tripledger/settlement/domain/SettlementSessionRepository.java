package com.rkdevstudios.tripledger.settlement.domain;

import java.util.List;
import java.util.Optional;

public interface SettlementSessionRepository {
    SettlementSession save(SettlementSession session);
    Optional<SettlementSession> findById(String id);
    List<SettlementSession> findByWorkspaceId(String workspaceId);
}
