package com.rkdevstudios.tripledger.settlement.domain;

import java.util.List;

public record SettlementPlan(
    String sessionId,
    String workspaceId,
    List<SettlementTransfer> transfers,
    String stateHash,
    int planVersion
) {}
