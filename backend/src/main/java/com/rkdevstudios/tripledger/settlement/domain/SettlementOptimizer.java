package com.rkdevstudios.tripledger.settlement.domain;

import java.util.List;

public interface SettlementOptimizer {
    List<SettlementTransfer> optimize(List<MemberBalance> balances, String currency);
}
