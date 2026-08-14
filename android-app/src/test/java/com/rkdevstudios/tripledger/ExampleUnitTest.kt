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
}