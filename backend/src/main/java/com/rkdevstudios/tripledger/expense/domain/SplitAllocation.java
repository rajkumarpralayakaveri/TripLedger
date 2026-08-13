package com.rkdevstudios.tripledger.expense.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "split_allocations")
public class SplitAllocation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "expense_id", nullable = false, length = 36)
    private String expenseId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Embedded
    private Money money;

    /**
     * Value representation depends on SplitType:
     * - Percentage split: holds the percent value (e.g. 20.0)
     * - Shares split: holds the share count (e.g. 3.0)
     * - Exact split: holds the exact amount (e.g. 500.0)
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal value;

    public SplitAllocation() {
    }

    public SplitAllocation(String id, String expenseId, String userId, Money money, BigDecimal value) {
        this.id = id;
        this.expenseId = expenseId;
        this.userId = userId;
        this.money = money;
        this.value = value;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getExpenseId() { return expenseId; }
    public void setExpenseId(String expenseId) { this.expenseId = expenseId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Money getMoney() { return money; }
    public void setMoney(Money money) { this.money = money; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
}
