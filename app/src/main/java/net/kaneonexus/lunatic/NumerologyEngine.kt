package net.kaneonexus.lunatic

import java.util.Calendar
import java.util.Date

/**
 * Numerology calculations: Life Path, Destiny (from name), and Daily Vibration.
 * Master numbers 11, 22, 33 are preserved (not reduced).
 */
object NumerologyEngine {

    private val letterValues = mapOf(
        'A' to 1, 'B' to 2, 'C' to 3, 'D' to 4, 'E' to 5, 'F' to 6, 'G' to 7, 'H' to 8, 'I' to 9,
        'J' to 1, 'K' to 2, 'L' to 3, 'M' to 4, 'N' to 5, 'O' to 6, 'P' to 7, 'Q' to 8, 'R' to 9,
        'S' to 1, 'T' to 2, 'U' to 3, 'V' to 4, 'W' to 5, 'X' to 6, 'Y' to 7, 'Z' to 8
    )

    private fun reduce(n: Int): Int {
        var num = n
        while (num > 9 && num != 11 && num != 22 && num != 33) {
            num = num.toString().sumOf { it.digitToInt() }
        }
        return num
    }

    /** birthDate in yyyy, month(1-12), day */
    fun lifePathNumber(year: Int, month: Int, day: Int): Int {
        val y = reduce(year.toString().sumOf { it.digitToInt() })
        val m = reduce(month)
        val d = reduce(day)
        return reduce(y + m + d)
    }

    fun destinyNumber(fullName: String): Int {
        val sum = fullName.uppercase().filter { it.isLetter() }
            .sumOf { letterValues[it] ?: 0 }
        return reduce(sum)
    }

    fun dailyVibration(date: Date = Date()): Int {
        val cal = Calendar.getInstance()
        cal.time = date
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val sum = y.toString().sumOf { it.digitToInt() } + m + d
        return reduce(sum)
    }

    fun meaningFor(number: Int): String = when (number) {
        1 -> "Initiation, independence, new beginnings"
        2 -> "Partnership, balance, receptivity"
        3 -> "Expression, creativity, communication"
        4 -> "Structure, discipline, groundwork"
        5 -> "Change, freedom, movement"
        6 -> "Responsibility, harmony, care"
        7 -> "Reflection, analysis, inner knowing"
        8 -> "Power, abundance, mastery of material plane"
        9 -> "Completion, compassion, release"
        11 -> "Master number: intuition, illumination"
        22 -> "Master number: the master builder"
        33 -> "Master number: the master teacher"
        else -> "Unmapped frequency"
    }
}
