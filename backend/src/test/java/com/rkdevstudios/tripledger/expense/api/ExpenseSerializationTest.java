package com.rkdevstudios.tripledger.expense.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void testExpenseTimelineItem_SerializesReceiptUrlAndExpenseAtToJson() throws Exception {
        Instant now = Instant.parse("2026-08-20T12:58:06.091738Z");
        String receiptUrl = "https://res.cloudinary.com/demo/image/upload/sample.jpg";

        ExpenseTimelineItem item = new ExpenseTimelineItem(
                "exp_100",
                "beach",
                BigDecimal.valueOf(800),
                "INR",
                "usr_1",
                "Lapcare",
                "cat_food",
                "Food",
                "restaurant",
                "#FF5722",
                LocalDate.parse("2026-08-20"),
                now,
                receiptUrl,
                "Sunset snacks"
        );

        String json = objectMapper.writeValueAsString(item);

        assertTrue(json.contains("\"receiptUrl\":\"https://res.cloudinary.com/demo/image/upload/sample.jpg\""));
        assertTrue(json.contains("\"expenseAt\":"));
        assertTrue(json.contains("\"paidByUserId\":\"usr_1\""));
        assertTrue(json.contains("\"paidByName\":\"Lapcare\""));
        assertTrue(json.contains("\"expenseDate\":"));
    }
}
