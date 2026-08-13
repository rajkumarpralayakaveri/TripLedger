package com.rkdevstudios.tripledger.settlement.domain;

public record SettlementConfirmedEvent(
    SettlementTransaction transaction
) {}
