package com.rkdevstudios.tripledger.expense.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "expense_categories")
public class ExpenseCategory {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "workspace_id", length = 36)
    private String workspaceId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String icon;

    @Column(length = 7)
    private String color;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "is_system_category", nullable = false)
    private boolean isSystemCategory = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ExpenseCategory() {
    }

    public ExpenseCategory(String id, String workspaceId, String name, String icon, String color, boolean isSystemCategory) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.isSystemCategory = isSystemCategory;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isSystemCategory() { return isSystemCategory; }
    public void setSystemCategory(boolean systemCategory) { isSystemCategory = systemCategory; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
