package net.kaneonexus.lunatic

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import net.kaneonexus.lunatic.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var journalStore: JournalStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        journalStore = JournalStore(this)

        renderMoonState()
        renderDailyVibration()
        renderTarotDraw()
        renderJournalHistory()

        binding.saveEntryButton.setOnClickListener { saveJournalEntry() }
    }

    private fun renderMoonState() {
        val moon = MoonEngine.currentState()
        binding.phaseNameText.text = moon.phaseName
        binding.illuminationText.text = "Illumination: ${moon.illuminationPct}%  ·  Age: %.1f days".format(moon.ageDays)
        binding.gravIndexText.text = "Gravitational Index: ${moon.gravitationalIndex}/100 — ${moon.gravitationalLabel}"

        val daysToFull = MoonEngine.daysUntilNextFullMoon()
        binding.nextFullMoonText.text = if (daysToFull == 0) "Full Moon is today"
            else "Next Full Moon in $daysToFull day(s)"
    }

    private fun renderDailyVibration() {
        val vibration = NumerologyEngine.dailyVibration()
        val meaning = NumerologyEngine.meaningFor(vibration)
        binding.dailyVibrationText.text = "Today's number: $vibration — $meaning"
    }

    private fun renderTarotDraw() {
        // Deterministic per-day seed so refreshing doesn't change today's card
        val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        val seed = dayFmt.format(java.util.Date()).toLong()
        val card = TarotEngine.drawDaily(seed)
        binding.tarotCardText.text = "${card.name} (${card.arcana})"
        binding.tarotKeywordText.text = card.keyword
    }

    private fun saveJournalEntry() {
        val moodText = binding.moodScoreInput.text.toString()
        val note = binding.noteInput.text.toString()

        val mood = moodText.toIntOrNull()
        if (mood == null || mood !in 1..10) {
            Toast.makeText(this, "Enter a mood score from 1-10", Toast.LENGTH_SHORT).show()
            return
        }

        journalStore.addEntry(mood, note)
        binding.moodScoreInput.text.clear()
        binding.noteInput.text.clear()
        renderJournalHistory()
        Toast.makeText(this, "Entry saved", Toast.LENGTH_SHORT).show()
    }

    private fun renderJournalHistory() {
        val entries = journalStore.allEntries().take(10)
        if (entries.isEmpty()) {
            binding.journalHistoryText.text = "No entries yet."
            return
        }
        binding.journalHistoryText.text = entries.joinToString("\n\n") { e ->
            "${e.timestamp} — Mood ${e.moodScore}/10 — ${e.moonPhase} (Vib ${e.dailyVibration})\n${e.note}"
        }
    }
}
