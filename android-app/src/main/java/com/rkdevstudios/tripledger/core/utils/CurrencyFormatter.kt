package com.rkdevstudios.tripledger.core.utils

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

    fun formatMoney(amount: BigDecimal?, currencyCode: String?): String {
        val nonNullAmount = amount ?: BigDecimal.ZERO
        val nonNullCurrency = currencyCode?.uppercase() ?: "INR"

        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,##0.00", symbols)
        val formattedAmount = formatter.format(nonNullAmount)

        val symbol = when (nonNullCurrency) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> nonNullCurrency
        }
        return "$symbol$formattedAmount"
    }
}
