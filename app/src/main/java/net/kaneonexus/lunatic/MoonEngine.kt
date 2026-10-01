package net.kaneonexus.lunatic

import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import kotlin.math.cos

/**
 * Core lunar calculation engine for LUNA-TIC.
 * Pure astronomical math — no external API required.
 */
object MoonEngine {

    // Known new moon reference point: Jan 6, 2000, 18:14 UTC
    private const val KNOWN_NEW_MOON_MS = 947182440000L
    private const val SYNODIC_MONTH_DAYS = 29.53058867

    data class MoonState(
        val ageDays: Double,
        val phaseName: String,
        val illuminationPct: Int,
        val gravitationalIndex: Int, // 0-100, push/pull intensity
        val gravitationalLabel: String
    )

    fun currentState(date: Date = Date()): MoonState {
        val diffMs = date.time - KNOWN_NEW_MOON_MS
        val diffDays = diffMs / (1000.0 * 60 * 60 * 24)
        var age = diffDays % SYNODIC_MONTH_DAYS
        if (age < 0) age += SYNODIC_MONTH_DAYS

        val phaseFraction = age / SYNODIC_MONTH_DAYS // 0..1
        val illumination = ((1 - cos(2 * Math.PI * phaseFraction)) / 2 * 100).toInt()

        val phaseName = phaseNameFor(phaseFraction)

        // Gravitational push/pull index: peaks at new moon (solar-lunar alignment)
        // and full moon (opposition), troughs at quarters.
        val gravIndex = (abs(cos(2 * Math.PI * phaseFraction)) * 100).toInt()
        val gravLabel = when {
            gravIndex >= 80 -> "Peak Tidal Pull"
            gravIndex >= 55 -> "Rising Tidal Pressure"
            gravIndex >= 30 -> "Moderate Flow"
            else -> "Low Tidal Tension"
        }

        return MoonState(age, phaseName, illumination, gravIndex, gravLabel)
    }

    private fun phaseNameFor(fraction: Double): String {
        return when {
            fraction < 0.03 || fraction > 0.97 -> "New Moon"
            fraction < 0.22 -> "Waxing Crescent"
            fraction < 0.28 -> "First Quarter"
            fraction < 0.47 -> "Waxing Gibbous"
            fraction < 0.53 -> "Full Moon"
            fraction < 0.72 -> "Waning Gibbous"
            fraction < 0.78 -> "Last Quarter"
            else -> "Waning Crescent"
        }
    }

    fun daysUntilNextFullMoon(from: Date = Date()): Int {
        val cal = Calendar.getInstance()
        cal.time = from
        for (i in 0..40) {
            val state = currentState(cal.time)
            if (state.phaseName == "Full Moon") return i
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return -1
    }
}
