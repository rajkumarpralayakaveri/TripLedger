package com.rkdevstudios.tripledger.contribution.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable ledger entry. Records are never updated or deleted.
 * Corrections are made by appending a new ADJUSTMENT entry.
 */
@Entity
@Table(name = "contribution_entries")
public class ContributionEntry {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "workspace_id", nullable = false, length = 36)
    private String workspaceId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private ContributionEntryType entryType;

    /**
     * Amount of the contribution. Negative values are allowed for ADJUSTMENT entries.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Links to the expense record when entryType == DIRECT_EXPENSE.
     */
    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ContributionEntry() {
    }

    public ContributionEntry(String id, String workspaceId, String userId,
                             ContributionEntryType entryType, BigDecimal amount,
                             String description, String referenceId) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.entryType = entryType;
        this.amount = amount;
        this.description = description;
        this.referenceId = referenceId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public ContributionEntryType getEntryType() { return entryType; }
    public void setEntryType(ContributionEntryType entryType) { this.entryType = entryType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
