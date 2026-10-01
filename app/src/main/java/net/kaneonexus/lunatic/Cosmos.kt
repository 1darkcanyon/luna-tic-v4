package net.kaneonexus.lunatic

import java.util.Date
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos

data class LunaMoon(val name: String, val frac: Double, val lit: Int, val age: Double, val energy: String, val advice: String)
data class LunaDraws(val tarot: Pair<String, String>, val rune: Pair<String, String>, val card: Pair<String, String>)

object Cosmos {
    private const val SYN = 29.53058867
    private val PHASES = listOf("New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent")
    private val ENERGY = listOf(
        "Reset, seed, intention" to "Quiet and reflective. Plant intentions and keep plans light.",
        "Sprout, momentum, hope" to "Curious and hopeful. Take small steps and avoid overcommitting.",
        "Action, challenge, build" to "Driven but tense. Make decisive moves and cut distractions.",
        "Refine, align, adjust" to "Analytical and purposeful. Review plans and fine-tune.",
        "Peak, reveal, release" to "Intense and expressive. Celebrate wins and release what is heavy.",
        "Harvest, integrate, teach" to "Grateful and insightful. Share what you have learned.",
        "Reassess, simplify, boundaries" to "Sober and discerning. Say no and clear clutter.",
        "Rest, surrender, dream" to "Sleepy and inward. Restore, journal and prepare for the reset.")

    fun moon(date: Date): LunaMoon {
        val jd = date.time / 86400000.0 + 2440587.5
        var f = ((jd - 2451550.1) / SYN) % 1.0
        if (f < 0) f += 1.0
        val i = ((f * 8) + 0.5).toInt() % 8
        val lit = Math.round((1 - cos(2 * PI * f)) / 2 * 100).toInt()
        val age = Math.round(f * SYN * 10) / 10.0
        return LunaMoon(PHASES[i], f, lit, age, ENERGY[i].first, ENERGY[i].second)
    }

    private val LIFE = mapOf(
        1 to "Leader, pioneer, independent force of will.", 2 to "Peacemaker, intuitive, emotionally intelligent.",
        3 to "Creative communicator, joyful and expressive.", 4 to "Builder, grounded, stability through work.",
        5 to "Adventurer, freedom seeker, change agent.", 6 to "Nurturer, healer, home and heart focused.",
        7 to "Seeker of truth, spiritual analyst, deep thinker.", 8 to "Powerful manifestor, balanced with karma.",
        9 to "Humanitarian, wise old soul, emotional depth.", 11 to "Spiritual illuminator, sensitive channel.",
        22 to "Master builder, grounded visionary.", 33 to "Master teacher, divine nurturer of all.")

    private fun digits(n: Int): Int = n.toString().map { it - '0' }.sum()

    fun lifePath(m: Int, d: Int, y: Int): Int {
        var t = digits(m) + digits(d) + digits(y)
        while (t > 9 && t != 11 && t != 22 && t != 33) t = digits(t)
        return t
    }

    fun lifeMeaning(n: Int): String = LIFE[n] ?: "A rare soul number."

    private val TAROT = listOf(
        "The Fool" to "New beginnings, innocence.", "The Magician" to "Manifestation, resourcefulness.",
        "The High Priestess" to "Intuition, hidden knowledge.", "The Empress" to "Abundance, nurturing.",
        "The Emperor" to "Structure, authority.", "The Hierophant" to "Tradition, guidance.",
        "The Lovers" to "Union, meaningful choices.", "The Chariot" to "Willpower, forward drive.",
        "Strength" to "Quiet courage, compassion.", "The Hermit" to "Solitude, inner search.",
        "Wheel of Fortune" to "Cycles, turning luck.", "Justice" to "Truth, fair outcomes.",
        "The Hanged One" to "Surrender, new perspective.", "Death" to "Endings that clear the way.",
        "Temperance" to "Balance, patience.", "The Devil" to "Attachments to look at honestly.",
        "The Tower" to "Sudden change, breakthrough.", "The Star" to "Hope, renewal.",
        "The Moon" to "Illusions, intuition, dreams.", "The Sun" to "Joy, vitality, success.",
        "Judgement" to "Reckoning, calling.", "The World" to "Completion, wholeness.")

    private val RUNES = listOf(
        "Fehu" to "Wealth, energy flowing.", "Uruz" to "Strength, raw vitality.", "Thurisaz" to "Defense, a threshold.",
        "Ansuz" to "Communication, divine messages.", "Raidho" to "Journey, soul travel.", "Kenaz" to "Torch, insight.",
        "Gebo" to "Gift, exchange.", "Wunjo" to "Joy, harmony.", "Hagalaz" to "Disruption that clears.",
        "Nauthiz" to "Need, endurance.", "Isa" to "Stillness, pause.", "Jera" to "Harvest, right timing.",
        "Eihwaz" to "Resilience, the yew.", "Perthro" to "Mystery, fate.", "Algiz" to "Protection.",
        "Sowilo" to "Sun, success.", "Tiwaz" to "Justice, courage.", "Berkano" to "Growth, renewal.",
        "Ehwaz" to "Trust, partnership.", "Mannaz" to "Self, community.", "Laguz" to "Flow, intuition.",
        "Ingwaz" to "Seed, gestation.", "Dagaz" to "Dawn, breakthrough.", "Othala" to "Home, heritage.")

    private val SUITS = listOf("Hearts" to "emotions and relationships", "Spades" to "transformation and clarity",
        "Diamonds" to "work, money and collaboration", "Clubs" to "energy, drive and creativity")
    private val RANKS = listOf("Ace" to "A fresh start in", "2" to "Balance and choice in", "3" to "Growth in",
        "4" to "Stability in", "5" to "Change or tension in", "6" to "Harmony in", "7" to "Reflection on",
        "8" to "Movement in", "9" to "Near completion of", "10" to "Fulfilment in", "Jack" to "A messenger for",
        "Queen" to "Mastery and care in", "King" to "Authority over")
    private val CARDS: List<Pair<String, String>> = SUITS.flatMap { s -> RANKS.map { r -> "${r.first} of ${s.first}" to "${r.second} ${s.second}." } }

    fun draws(name: String, day: String): LunaDraws {
        val r = Random("$name|$day".hashCode().toLong())
        return LunaDraws(TAROT[r.nextInt(TAROT.size)], RUNES[r.nextInt(RUNES.size)], CARDS[r.nextInt(CARDS.size)])
    }

    fun nexusMessage(name: String, m: LunaMoon, lp: Int): String =
        "$name, under the ${m.name} your energy leans toward ${m.energy.lowercase()}. Life Path $lp: ${lifeMeaning(lp)} ${m.advice}"
}
