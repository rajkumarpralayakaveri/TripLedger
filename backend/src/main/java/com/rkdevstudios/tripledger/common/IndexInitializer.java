package com.rkdevstudios.tripledger.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;

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
                } else {
                    logger.info("Non-PostgreSQL database detected ({}); skipping partial unique index DDL.", dbName);
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to execute database index initialization: ", e);
        }
    }
}
