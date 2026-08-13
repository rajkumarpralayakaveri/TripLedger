package com.rkdevstudios.tripledger.settlement.api;

import com.rkdevstudios.tripledger.expense.domain.Money;
import java.util.List;

public record SettlementPlanResponse(
    String sessionId,
    String workspaceId,
    List<SettlementTransferResponse> transfers,
    String stateHash,
    int planVersion
) {
    public record SettlementTransferResponse(
        String id,
        String fromUserId,
        String fromUserName,
        String toUserId,
        String toUserName,
        Money amount
    ) {}
}
