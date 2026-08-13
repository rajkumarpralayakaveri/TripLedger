package com.rkdevstudios.tripledger.settlement.domain;

import com.rkdevstudios.tripledger.expense.domain.Money;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "settlement_transactions")
public class SettlementTransaction {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "workspace_id", nullable = false, length = 36)
    private String workspaceId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "from_user_id", nullable = false, length = 36)
    private String fromUserId;

    @Column(name = "to_user_id", nullable = false, length = 36)
    private String toUserId;

    @Embedded
    private Money money;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public SettlementTransaction() {
    }

    public SettlementTransaction(String id, String workspaceId, String sessionId, String fromUserId,
                                 String toUserId, Money money) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.sessionId = sessionId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.money = money;
        this.status = SettlementStatus.PENDING;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public Money getMoney() { return money; }
    public void setMoney(Money money) { this.money = money; }

    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}
