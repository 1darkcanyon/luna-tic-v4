package net.kaneonexus.lunatic

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simple local journal store. Each entry ties a mood/note to the lunar
 * and numerological state on that day, building the personal correlation
 * dataset described in the vision doc.
 */
class JournalStore(context: Context) {

    private val prefs = context.getSharedPreferences("luna_tic_journal", Context.MODE_PRIVATE)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    data class Entry(
        val timestamp: String,
        val moodScore: Int, // 1-10
        val note: String,
        val moonPhase: String,
        val gravIndex: Int,
        val dailyVibration: Int
    )

    fun addEntry(moodScore: Int, note: String) {
        val moon = MoonEngine.currentState()
        val vibration = NumerologyEngine.dailyVibration()

        val entry = JSONObject().apply {
            put("timestamp", dateFmt.format(Date()))
            put("moodScore", moodScore)
            put("note", note)
            put("moonPhase", moon.phaseName)
            put("gravIndex", moon.gravitationalIndex)
            put("dailyVibration", vibration)
        }

        val arr = readRawArray()
        arr.put(entry)
        prefs.edit().putString("entries", arr.toString()).apply()
    }

    fun allEntries(): List<Entry> {
        val arr = readRawArray()
        val list = mutableListOf<Entry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Entry(
                    timestamp = o.getString("timestamp"),
                    moodScore = o.getInt("moodScore"),
                    note = o.getString("note"),
                    moonPhase = o.getString("moonPhase"),
                    gravIndex = o.getInt("gravIndex"),
                    dailyVibration = o.getInt("dailyVibration")
                )
            )
        }
        return list.reversed() // most recent first
    }

    private fun readRawArray(): JSONArray {
        val raw = prefs.getString("entries", "[]") ?: "[]"
        return JSONArray(raw)
    }
}
