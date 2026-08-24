package com.rkdevstudios.tripledger

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun testWorkspaceFinancialSnapshotDeserialization() {
        val json = """
            {
                "workspaceId": "ws_1",
                "totalBudget": 35000.0,
                "totalSpent": 0.0,
                "remainingBudget": 35000.0,
                "currentFund": 0.0,
                "fundingGap": 35000.0,
                "memberCount": 2,
                "fundedMembers": 0,
                "pendingMembers": 2,
                "overFundedMembers": 0,
                "memberContributions": [
                    {
                        "userId": "usr_1",
                        "name": "Owner",
                        "role": "OWNER",
                        "plannedContribution": 7000.0,
                        "totalContribution": 0.0,
                        "remainingContribution": 7000.0,
                        "status": "NOT_STARTED"
                    },
                    {
                        "userId": "usr_2",
                        "name": "Member",
                        "role": "MEMBER",
                        "plannedContribution": 7000.0,
                        "totalContribution": 0.0,
                        "remainingContribution": 7000.0,
                        "status": "NOT_STARTED"
                    }
                ]
            }
        """.trimIndent()

        val gson = com.google.gson.Gson()
        val dto = gson.fromJson(json, com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceFinancialSnapshotDto::class.java)

        assertEquals("ws_1", dto.workspaceId)
        assertEquals(2, dto.memberContributions.size)
        
        val owner = dto.memberContributions[0]
        assertEquals("usr_1", owner.userId)
        assertEquals("Owner", owner.name)
        assertEquals("OWNER", owner.role)
        assertEquals(0, java.math.BigDecimal("7000.0").compareTo(owner.planned))
        assertEquals(0, java.math.BigDecimal("0.0").compareTo(owner.total))
        assertEquals(0, java.math.BigDecimal("7000.0").compareTo(owner.remaining))
        assertEquals("NOT_STARTED", owner.status)
    }

    @Test
    fun testPaymentProofResponseDtoDeserialization() {
        val json = """
            {
                "id": "proof_123",
                "workspaceId": "ws_1",
                "userId": "usr_1",
                "payerName": "Jack",
                "amount": 600.00,
                "status": "PENDING",
                "createdAt": "2026-08-19T13:20:15Z",
                "submittedAt": "2026-08-19T13:20:15Z",
                "verifiedAt": null,
                "verifiedBy": null,
                "rejectionReason": null,
                "viewUrl": "https://res.cloudinary.com/demo/image/upload/v1580518928/sample.jpg"
            }
        """.trimIndent()

        val gson = com.google.gson.Gson()
        val dto = gson.fromJson(json, com.rkdevstudios.tripledger.features.workspace.data.api.PaymentProofResponseDto::class.java)

        assertEquals("proof_123", dto.id)
        assertEquals("Jack", dto.payerName)
        assertEquals("PENDING", dto.status)
        assertEquals("https://res.cloudinary.com/demo/image/upload/v1580518928/sample.jpg", dto.viewUrl)
    }

    @Test
    fun testSettlementPlanDeserialization() {
        val json = """
            {
                "sessionId": "sess_123",
                "workspaceId": "ws_1",
                "transfers": [
                    {
                        "id": "t_1",
                        "fromUserId": "usr_member",
                        "fromUserName": "Regular Member",
                        "toUserId": "usr_admin",
                        "toUserName": "Admin User",
                        "amount": {
                            "amount": 33.3333,
                            "currency": "INR"
                        }
                    }
                ],
                "stateHash": "hash1",
                "planVersion": 1
            }
        """.trimIndent()

        val gson = com.google.gson.Gson()
        val dto = gson.fromJson(json, com.rkdevstudios.tripledger.features.workspace.data.api.SettlementPlanResponseDto::class.java)

        assertEquals("sess_123", dto.sessionId)
        assertEquals(1, dto.transfers.size)
        
        val transfer = dto.transfers[0]
        assertEquals("t_1", transfer.id)
        assertEquals("usr_member", transfer.fromUserId)
        assertEquals("Regular Member", transfer.fromUserName)
        assertEquals("usr_admin", transfer.toUserId)
        assertEquals("Admin User", transfer.toUserName)
        assertEquals(0, java.math.BigDecimal("33.3333").compareTo(transfer.amount.amount))
        assertEquals("INR", transfer.amount.currency)
    }
}