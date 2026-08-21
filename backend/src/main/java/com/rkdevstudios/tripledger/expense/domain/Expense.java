package com.rkdevstudios.tripledger.expense.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "workspace_id", nullable = false, length = 36)
    private String workspaceId;

    @Column(name = "paid_by_user_id", nullable = false, length = 36)
    private String paidByUserId;

    @Embedded
    private Money money;

    @Column(nullable = false)
    private String description;

    @Column(name = "category_id", nullable = false, length = 36)
    private String categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseStatus status = ExpenseStatus.UNSETTLED;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 20)
    private ExpenseType expenseType = ExpenseType.NORMAL;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "expense_at", nullable = false)
    private Instant expenseAt = Instant.now();

    @Column(name = "receipt_url", length = 512)
    private String receiptUrl;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'EQUAL'")
    private SplitType splitType = SplitType.EQUAL;

    @Column(name = "created_by_user_id", length = 36)
    private String createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Expense() {
    }

    public Expense(String id, String workspaceId, String paidByUserId, Money money, String description,
                   String categoryId, LocalDate expenseDate) {
        this(id, workspaceId, paidByUserId, money, description, categoryId, expenseDate, Instant.now(), null, null, SplitType.EQUAL, null);
    }

    public Expense(String id, String workspaceId, String paidByUserId, Money money, String description,
                   String categoryId, LocalDate expenseDate, Instant expenseAt, String receiptUrl, String note, SplitType splitType) {
        this(id, workspaceId, paidByUserId, money, description, categoryId, expenseDate, expenseAt, receiptUrl, note, splitType, null);
    }

    public Expense(String id, String workspaceId, String paidByUserId, Money money, String description,
                   String categoryId, LocalDate expenseDate, Instant expenseAt, String receiptUrl, String note, SplitType splitType, String createdByUserId) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.paidByUserId = paidByUserId;
        this.money = money;
        this.description = description;
        this.categoryId = categoryId;
        this.expenseDate = expenseDate;
        this.expenseAt = expenseAt != null ? expenseAt : Instant.now();
        this.receiptUrl = receiptUrl;
        this.note = note;
        this.splitType = splitType != null ? splitType : SplitType.EQUAL;
        this.createdByUserId = createdByUserId;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getPaidByUserId() { return paidByUserId; }
    public void setPaidByUserId(String paidByUserId) { this.paidByUserId = paidByUserId; }

    public Money getMoney() { return money; }
    public void setMoney(Money money) { this.money = money; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public ExpenseStatus getStatus() { return status; }
    public void setStatus(ExpenseStatus status) { this.status = status; }

    public ExpenseType getExpenseType() { return expenseType; }
    public void setExpenseType(ExpenseType expenseType) { this.expenseType = expenseType; }

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

    public Instant getExpenseAt() { return expenseAt; }
    public void setExpenseAt(Instant expenseAt) { this.expenseAt = expenseAt; }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public SplitType getSplitType() { return splitType; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }

    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }
}
