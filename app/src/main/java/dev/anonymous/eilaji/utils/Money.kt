package dev.anonymous.eilaji.utils

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

/**
 * Locale aware money formatting helper.
 *
 * Keeps the currency symbol on the trailing side of the amount (the app is Arabic/RTL first),
 * while still grouping/thousands-separating using the device locale, and scales correctly with
 * the user font scale because it produces plain [CharSequence] values.
 */
object Money {

    /** The symbol shown next to every price in the app. */
    fun symbol(context: Context): String = context.getString(dev.anonymous.eilaji.R.string.currency_symbol)

    /**
     * Formats [amount] as e.g. `12.50 $` (LTR locales) / `12.50 $` (RTL, mirrored direction).
     * Always keeps the symbol after the number so it never lands in the middle of the digits.
     */
    fun format(amount: Double, context: Context): String {
        val number = numberFormat().format(amount)
        return context.getString(dev.anonymous.eilaji.R.string.price_format, number, symbol(context))
    }

    /** Formats a whole-line total (price x quantity) with the same rules as [format]. */
    fun formatTotal(amount: Double, context: Context): String = format(amount, context)

    private fun numberFormat(): NumberFormat {
        return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
}
