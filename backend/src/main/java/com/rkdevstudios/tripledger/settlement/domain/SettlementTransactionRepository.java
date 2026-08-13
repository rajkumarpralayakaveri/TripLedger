package com.rkdevstudios.tripledger.settlement.domain;

import java.util.List;
import java.util.Optional;

public interface SettlementTransactionRepository {
    SettlementTransaction save(SettlementTransaction transaction);
    Optional<SettlementTransaction> findById(String id);
    List<SettlementTransaction> findByWorkspaceId(String workspaceId);
}
