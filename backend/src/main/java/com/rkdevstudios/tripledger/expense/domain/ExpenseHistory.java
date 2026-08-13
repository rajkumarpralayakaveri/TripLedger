package com.rkdevstudios.tripledger.expense.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "expense_history")
public class ExpenseHistory {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "expense_id", nullable = false, length = 36)
    private String expenseId;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "before_json", columnDefinition = "TEXT")
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "TEXT")
    private String afterJson;

    @Column(name = "actor_user_id", nullable = false, length = 36)
    private String actorUserId;

    private String reason;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    public ExpenseHistory() {
    }

    public ExpenseHistory(String id, String expenseId, String action, String beforeJson, String afterJson,
                          String actorUserId, String reason) {
        this.id = id;
        this.expenseId = expenseId;
        this.action = action;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.actorUserId = actorUserId;
        this.reason = reason;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getExpenseId() { return expenseId; }
    public void setExpenseId(String expenseId) { this.expenseId = expenseId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getBeforeJson() { return beforeJson; }
    public void setBeforeJson(String beforeJson) { this.beforeJson = beforeJson; }

    public String getAfterJson() { return afterJson; }
    public void setAfterJson(String afterJson) { this.afterJson = afterJson; }

    public String getActorUserId() { return actorUserId; }
    public void setActorUserId(String actorUserId) { this.actorUserId = actorUserId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
