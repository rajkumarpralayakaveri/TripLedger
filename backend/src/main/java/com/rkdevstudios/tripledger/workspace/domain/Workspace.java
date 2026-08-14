package com.rkdevstudios.tripledger.workspace.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.rkdevstudios.tripledger.contribution.domain.ContributionStrategy;

@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(precision = 19, scale = 4)
    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspaceStatus status = WorkspaceStatus.PLANNING;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_strategy", nullable = false, length = 20)
    private ContributionStrategy contributionStrategy = ContributionStrategy.EQUAL;

    @Column(name = "planned_member_count")
    private Integer plannedMemberCount = 1;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();


    public Workspace() {
    }

    public Workspace(String id, String name, String description, LocalDate startDate, LocalDate endDate,
                     String baseCurrency, BigDecimal budget, Integer plannedMemberCount, String createdBy) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.baseCurrency = baseCurrency;
        this.budget = budget;
        this.plannedMemberCount = plannedMemberCount != null ? plannedMemberCount : 1;
        this.createdBy = createdBy;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public WorkspaceStatus getStatus() {
        return status;
    }

    public void setStatus(WorkspaceStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ContributionStrategy getContributionStrategy() {
        return contributionStrategy;
    }

    public void setContributionStrategy(ContributionStrategy contributionStrategy) {
        this.contributionStrategy = contributionStrategy;
    }

    public Integer getPlannedMemberCount() {
        return plannedMemberCount != null ? plannedMemberCount : 1;
    }

    public void setPlannedMemberCount(Integer plannedMemberCount) {
        this.plannedMemberCount = plannedMemberCount;
    }
}
