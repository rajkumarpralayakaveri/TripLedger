package com.rkdevstudios.tripledger.expense.api;

import com.rkdevstudios.tripledger.expense.domain.ActivityEntry;
import com.rkdevstudios.tripledger.expense.domain.ActivityType;
import java.time.Instant;

public record ActivityResponse(
    String id,
    String workspaceId,
    String userId,
    String userName,
    ActivityType activityType,
    String metadataJson,
    String message,
    Instant createdAt
) {
    public static ActivityResponse fromDomain(ActivityEntry entry, String userName, String descriptionDetail) {
        String msg = switch (entry.getActivityType()) {
            case EXPENSE_CREATED -> userName + " added " + (descriptionDetail != null ? descriptionDetail : "an expense");
            case EXPENSE_UPDATED -> userName + " edited " + (descriptionDetail != null ? descriptionDetail : "an expense");
            case EXPENSE_DELETED -> userName + " deleted " + (descriptionDetail != null ? descriptionDetail : "an expense");
            case MEMBER_JOINED -> userName + " joined workspace";
            case WORKSPACE_CREATED -> userName + " created workspace";
            case SETTLEMENT_CONFIRMED -> userName + " paid " + (descriptionDetail != null ? descriptionDetail : "a settlement repayment");
            case PAYMENT_SUBMITTED -> userName + " submitted a payment proof" + (descriptionDetail != null ? " of " + descriptionDetail : "");
            case PAYMENT_APPROVED -> userName + "'s payment" + (descriptionDetail != null ? " of " + descriptionDetail : "") + " was approved";
            case PAYMENT_REJECTED -> userName + "'s payment was rejected";
        };

        return new ActivityResponse(
                entry.getId(),
                entry.getWorkspaceId(),
                entry.getUserId(),
                userName,
                entry.getActivityType(),
                entry.getMetadataJson(),
                msg,
                entry.getCreatedAt()
        );
    }
}
