package net.kaneonexus.lunatic

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local journal store. Holds mood/note entries and saved readings.
 * Entries written by earlier builds still load: missing fields fall back to defaults.
 */
class JournalStore(context: Context) {

    private val prefs = context.getSharedPreferences("luna_tic_journal", Context.MODE_PRIVATE)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    data class Entry(
        val timestamp: String,
        val moodScore: Int, // 1-10 for journal entries, 0 for readings
        val note: String,
        val moonPhase: String,
        val gravIndex: Int,
        val dailyVibration: Int,
        val kind: String = "journal", // "journal" or "reading"
        val title: String = "",
        val profile: String = ""
    )

    fun addEntry(moodScore: Int, note: String, profile: String = "") {
        val o = newBase()
        o.put("moodScore", moodScore)
        o.put("note", note)
        o.put("kind", "journal")
        o.put("profile", profile)
        append(o)
    }

    fun addReading(title: String, body: String, profile: String = "") {
        val o = newBase()
        o.put("moodScore", 0)
        o.put("note", body)
        o.put("kind", "reading")
        o.put("title", title)
        o.put("profile", profile)
        append(o)
    }

    /** Most recent first. If profile is given, shows that profile's entries plus older untagged ones. */
    fun allEntries(profile: String? = null): List<Entry> {
        val arr = readRawArray()
        val list = mutableListOf<Entry>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val e = Entry(
                timestamp = o.optString("timestamp", ""),
                moodScore = o.optInt("moodScore", 0),
                note = o.optString("note", ""),
                moonPhase = o.optString("moonPhase", ""),
                gravIndex = o.optInt("gravIndex", 0),
                dailyVibration = o.optInt("dailyVibration", 0),
                kind = o.optString("kind", "journal"),
                title = o.optString("title", ""),
                profile = o.optString("profile", "")
            )
            if (profile == null || e.profile.isEmpty() || e.profile == profile) list.add(e)
        }
        return list.reversed()
    }

    /** Plain-text backup, oldest first, for sharing or saving elsewhere. */
    fun exportText(profile: String?): String {
        val sb = StringBuilder("LUNA-TIC journal\n\n")
        for (e in allEntries(profile).reversed()) {
            if (e.kind == "reading") {
                sb.append("[${e.timestamp}] ${e.title}\n")
            } else {
                sb.append("[${e.timestamp}] ${e.moonPhase} - mood ${e.moodScore}/10\n")
            }
            sb.append(e.note).append("\n\n")
        }
        return sb.toString().trimEnd()
    }

    private fun newBase(): JSONObject {
        val phase: String = try { Cosmos.moon(Date()).name } catch (e: Exception) { "" }
        val grav: Any = try { MoonEngine.currentState().gravitationalIndex } catch (e: Exception) { 0 }
        val vib: Any = try { NumerologyEngine.dailyVibration() } catch (e: Exception) { 0 }
        val o = JSONObject()
        o.put("timestamp", dateFmt.format(Date()))
        o.put("moonPhase", phase)
        o.put("gravIndex", grav)
        o.put("dailyVibration", vib)
        return o
    }

    private fun append(o: JSONObject) {
        val arr = readRawArray()
        arr.put(o)
        prefs.edit().putString("entries", arr.toString()).apply()
    }

    private fun readRawArray(): JSONArray {
        val raw = prefs.getString("entries", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }
}
