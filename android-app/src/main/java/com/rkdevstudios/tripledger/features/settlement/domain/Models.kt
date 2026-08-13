package com.rkdevstudios.tripledger.features.settlement.domain

import java.math.BigDecimal
import java.time.LocalDate

data class MemberBalanceItem(
    val userId: String,
    val userName: String,
    val paid: BigDecimal,
    val owed: BigDecimal,
    val balance: BigDecimal
)

data class SettlementTransferItem(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: BigDecimal,
    val currency: String
)

data class SettlementPlanItem(
    val sessionId: String,
    val workspaceId: String,
    val transfers: List<SettlementTransferItem>,
    val stateHash: String,
    val planVersion: Int
)

data class SettlementHistoryItem(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: BigDecimal,
    val currency: String,
    val confirmedAt: String
)

data class SettlementHistoryGroupItem(
    val date: LocalDate,
    val transactions: List<SettlementHistoryItem>
)
