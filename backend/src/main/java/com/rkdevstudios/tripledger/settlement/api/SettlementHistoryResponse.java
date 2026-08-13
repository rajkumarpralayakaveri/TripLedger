package com.rkdevstudios.tripledger.settlement.api;

import com.rkdevstudios.tripledger.expense.domain.Money;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public record SettlementHistoryResponse(
    List<SettlementHistoryGroup> groups
) {
    public record SettlementHistoryGroup(
        LocalDate date,
        List<HistoryItem> transactions
    ) {}

    public record HistoryItem(
        String id,
        String fromUserId,
        String fromUserName,
        String toUserId,
        String toUserName,
        Money amount,
        Instant confirmedAt
    ) {}
}
