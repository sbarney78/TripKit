package au.barney.tripkit.util

import java.util.Locale

object WeightUtils {
    fun formatWeight(grams: Int): String {
        return if (grams < 1000) {
            "${grams}g"
        } else {
            val kg = grams / 1000.0
            if (kg == kg.toInt().toDouble()) {
                "${kg.toInt()}kg"
            } else {
                String.format(Locale.getDefault(), "%.1fkg", kg)
            }
        }
    }
}
