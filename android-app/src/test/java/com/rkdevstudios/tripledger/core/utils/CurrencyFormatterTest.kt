package com.rkdevstudios.tripledger.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CurrencyFormatterTest {

    @Test
    fun testFormatINR() {
        val amount = BigDecimal("10000.00")
        val formatted = CurrencyFormatter.formatMoney(amount, "INR")
        assertEquals("₹10,000.00", formatted)
    }

    @Test
    fun testFormatUSD() {
        val amount = BigDecimal("10000.00")
        val formatted = CurrencyFormatter.formatMoney(amount, "USD")
        assertEquals("$10,000.00", formatted)
    }

    @Test
    fun testFormatEUR() {
        val amount = BigDecimal("10000.00")
        val formatted = CurrencyFormatter.formatMoney(amount, "EUR")
        assertEquals("€10,000.00", formatted)
    }

    @Test
    fun testFormatGBP() {
        val amount = BigDecimal("10000.00")
        val formatted = CurrencyFormatter.formatMoney(amount, "GBP")
        assertEquals("£10,000.00", formatted)
    }

    @Test
    fun testFormatZero() {
        val amount = BigDecimal.ZERO
        val formatted = CurrencyFormatter.formatMoney(amount, "USD")
        assertEquals("$0.00", formatted)
    }

    @Test
    fun testFormatDecimalValues() {
        val amount = BigDecimal("123.456")
        val formatted = CurrencyFormatter.formatMoney(amount, "USD")
        assertEquals("$123.46", formatted) // Bankers rounding / half-even/up formatting
    }

    @Test
    fun testFormatLargeValues() {
        val amount = BigDecimal("987654321.09")
        val formatted = CurrencyFormatter.formatMoney(amount, "INR")
        assertEquals("₹987,654,321.09", formatted)
    }

    @Test
    fun testFormatNullValues() {
        val formatted = CurrencyFormatter.formatMoney(null, null)
        assertEquals("₹0.00", formatted)
    }
}
