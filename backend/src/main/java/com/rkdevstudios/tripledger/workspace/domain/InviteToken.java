package com.rkdevstudios.tripledger.workspace.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "invite_tokens")
public class InviteToken {

    @Id
    @Column(length = 36)
    private String token;

    @Column(name = "workspace_id", nullable = false, length = 36)
    private String workspaceId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "current_uses", nullable = false)
    private int currentUses = 0;

    @Column(nullable = false)
    private boolean active = true;

    public InviteToken() {
    }

    public InviteToken(String token, String workspaceId, Instant expiresAt, String createdBy, int maxUses) {
        this.token = token;
        this.workspaceId = workspaceId;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.maxUses = maxUses;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsageLimitReached() {
        return maxUses > 0 && currentUses >= maxUses;
    }

    public boolean isValid() {
        return active && !isExpired() && !isUsageLimitReached();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(int maxUses) {
        this.maxUses = maxUses;
    }

    public int getCurrentUses() {
        return currentUses;
    }

    public void setCurrentUses(int currentUses) {
        this.currentUses = currentUses;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
