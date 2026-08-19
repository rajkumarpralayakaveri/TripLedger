package com.rkdevstudios.tripledger.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;

@Component
public class IndexInitializer {

    private static final Logger logger = LoggerFactory.getLogger(IndexInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public IndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() {
        try {
            jdbcTemplate.execute((Connection connection) -> {
                String dbName = connection.getMetaData().getDatabaseProductName();
                logger.info("Detecting database engine for unique cash entry index: {}", dbName);
                if ("PostgreSQL".equalsIgnoreCase(dbName)) {
                    logger.info("Applying PostgreSQL partial unique index constraint...");
                    jdbcTemplate.execute(
                            "CREATE UNIQUE INDEX IF NOT EXISTS uq_cash_entry_reference_id " +
                            "ON contribution_entries (reference_id) " +
                            "WHERE entry_type = 'CASH'"
                    );
                    logger.info("Successfully verified/created uq_cash_entry_reference_id index on PostgreSQL.");

                    logger.info("Updating PostgreSQL check constraint for activity_entries.activity_type...");
                    try {
                        List<String> checkConstraints = jdbcTemplate.queryForList(
                                "SELECT constraint_name FROM information_schema.table_constraints " +
                                "WHERE table_name = 'activity_entries' AND constraint_type = 'CHECK'",
                                String.class
                        );
                        for (String constraint : checkConstraints) {
                            logger.info("Dropping constraint: {}", constraint);
                            jdbcTemplate.execute("ALTER TABLE activity_entries DROP CONSTRAINT IF EXISTS " + constraint);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to drop existing check constraints dynamically; trying fallback drop: {}", e.getMessage());
                        jdbcTemplate.execute(
                                "ALTER TABLE activity_entries DROP CONSTRAINT IF EXISTS activity_entries_activity_type_check"
                        );
                    }

                    jdbcTemplate.execute(
                            "ALTER TABLE activity_entries ADD CONSTRAINT activity_entries_activity_type_check " +
                            "CHECK (activity_type IN ('EXPENSE_CREATED', 'EXPENSE_UPDATED', 'EXPENSE_DELETED', " +
                            "'MEMBER_JOINED', 'WORKSPACE_CREATED', 'SETTLEMENT_CONFIRMED', 'PAYMENT_SUBMITTED', " +
                            "'PAYMENT_APPROVED', 'PAYMENT_REJECTED'))"
                    );
                    logger.info("Successfully updated activity_entries_activity_type_check check constraint on PostgreSQL.");
                } else {
                    logger.info("Non-PostgreSQL database detected ({}); skipping partial unique index and check constraint DDL.", dbName);
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to execute database schema initialization: ", e);
        }
    }
}
