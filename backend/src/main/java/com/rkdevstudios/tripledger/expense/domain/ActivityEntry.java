package com.rkdevstudios.tripledger.expense.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "activity_entries")
public class ActivityEntry {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "workspace_id", nullable = false, length = 36)
    private String workspaceId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 25)
    private ActivityType activityType;

    // Metadata payload (e.g. details like 'Fuel' or expense IDs)
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ActivityEntry() {
    }

    public ActivityEntry(String id, String workspaceId, String userId, ActivityType activityType, String metadataJson) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.activityType = activityType;
        this.metadataJson = metadataJson;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType activityType) { this.activityType = activityType; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
