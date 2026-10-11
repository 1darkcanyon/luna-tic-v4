package net.kaneonexus.lunatic

import kotlin.random.Random

/**
 * Shared reading engine for LUNA-TIC.
 * One draw/spread system serves every deck (runes, tarot, playing cards),
 * so later decks only need a card list and a spread list.
 * All meanings are original text written for LUNA-TIC.
 */
object ReadingEngine {

    enum class Kind(val label: String) {
        RUNES("Runes"),
        TAROT("Tarot"),
        CARDS("Playing cards")
    }

    /** reversed == "" means the card cannot be reversed (e.g. symmetrical runes, playing cards). */
    data class ReadCard(
        val name: String,
        val upright: String,
        val reversed: String = "",
        val glyph: String = ""
    ) {
        val canReverse: Boolean get() = reversed.isNotEmpty()
    }

    data class Spread(val id: String, val name: String, val positions: List<String>)

    data class Drawn(val position: String, val card: ReadCard, val isReversed: Boolean) {
        val meaning: String get() = if (isReversed) card.reversed else card.upright
        val label: String
            get() {
                val g = if (card.glyph.isNotEmpty()) "${card.glyph} " else ""
                val r = if (isReversed) " (reversed)" else ""
                return "$g${card.name}$r"
            }
    }

    data class Reading(
        val kind: Kind,
        val spread: Spread,
        val question: String,
        val cards: List<Drawn>
    ) {
        val title: String get() = "${kind.label} \u00B7 ${spread.name}"

        fun toText(): String {
            val sb = StringBuilder()
            sb.append(title).append("\n")
            if (question.isNotEmpty()) sb.append("Q: ").append(question).append("\n")
            cards.forEachIndexed { i, d ->
                sb.append("\n${i + 1}. ${d.position}: ${d.label}\n${d.meaning}")
            }
            return sb.toString()
        }
    }

    // ---------------------------------------------------------------- spreads

    fun spreads(kind: Kind): List<Spread> = when (kind) {
        Kind.RUNES -> listOf(
            Spread("r1", "One rune", listOf("Guidance")),
            Spread("r3", "Three runes", listOf("Past", "Present", "Future")),
            Spread("r5", "Five runes", listOf("Situation", "Challenge", "Hidden influence", "Advice", "Outcome"))
        )
        Kind.TAROT -> listOf(
            Spread("t1", "One card", listOf("The card")),
            Spread("t3", "Three cards", listOf("Past", "Present", "Future")),
            Spread(
                "tcc", "Celtic Cross", listOf(
                    "The present", "The challenge", "The foundation", "The recent past",
                    "The crown (best outcome)", "The near future", "The self",
                    "Surroundings", "Hopes and fears", "The outcome"
                )
            )
        )
        Kind.CARDS -> listOf(
            Spread("c1", "One card", listOf("The card")),
            Spread("c3", "Three cards", listOf("Past", "Present", "Future")),
            Spread(
                "c9", "Nine-card square", listOf(
                    "Roots", "Recent past", "Fading influence",
                    "The present", "Heart of the matter", "Hidden factor",
                    "Coming soon", "Advice", "Outcome"
                )
            )
        )
    }

    fun deck(kind: Kind): List<ReadCard> = when (kind) {
        Kind.RUNES -> runeDeck
        Kind.TAROT -> tarotDeck
        Kind.CARDS -> playingDeck
    }

    // ---------------------------------------------------------------- drawing

    fun draw(
        kind: Kind,
        spread: Spread,
        question: String,
        allowReversed: Boolean,
        rng: Random = Random.Default
    ): Reading {
        val shuffled = deck(kind).shuffled(rng)
        val drawn = spread.positions.mapIndexed { i, pos ->
            val c = shuffled[i]
            val rev = allowReversed && c.canReverse && rng.nextBoolean()
            Drawn(pos, c, rev)
        }
        return Reading(kind, spread, question.trim(), drawn)
    }

    // ------------------------------------------------------------------ runes

    private val runeDeck: List<ReadCard> = listOf(
        ReadCard("Fehu", "Wealth in motion: resources, reward for effort, energy worth investing.",
            "Resources leaking away; effort without return, or value that has stopped flowing.", "\u16A0"),
        ReadCard("Uruz", "Raw strength and vitality: health, stamina, untamed potential.",
            "Depleted strength; force misused, or a lesson in patience.", "\u16A2"),
        ReadCard("Thurisaz", "A thorn at the gate: a defensive force, a necessary conflict, a test.",
            "Defenses turned inward; reckless provocation, or a danger being ignored.", "\u16A6"),
        ReadCard("Ansuz", "A message arrives: clear speech, wise counsel, inspiration.",
            "Mixed signals; misunderstanding, flattery, or advice not to be trusted.", "\u16A8"),
        ReadCard("Raidho", "The road and its rhythm: travel, right action, steady progress.",
            "Delays and detours; a plan out of step with its timing.", "\u16B1"),
        ReadCard("Kenaz", "The torch: insight, craft, a creative fire that lights the way.",
            "A guttering flame; creativity blocked, knowledge withheld.", "\u16B2"),
        ReadCard("Gebo", "The gift: exchange, partnership, a balance of giving and receiving.", "", "\u16B7"),
        ReadCard("Wunjo", "Joy and belonging: harmony, wishes fulfilled, shared contentment.",
            "Joy withheld; alienation, or happiness postponed by strife.", "\u16B9"),
        ReadCard("Hagalaz", "Hail: disruption beyond your control that clears the ground for something new.", "", "\u16BA"),
        ReadCard("Nauthiz", "Need-fire: constraint and necessity that teach endurance and resourcefulness.", "", "\u16BE"),
        ReadCard("Isa", "Ice: a pause, stillness, patience while things freeze in place.", "", "\u16C1"),
        ReadCard("Jera", "Harvest: the reward of a full cycle, patience repaid in season.", "", "\u16C3"),
        ReadCard("Eihwaz", "The yew: endurance, protection, the bridge between endings and beginnings.", "", "\u16C7"),
        ReadCard("Perthro", "The dice cup: mystery, chance, hidden things coming to light.",
            "Secrets staying hidden; a bad gamble, stagnation, or fear of the unknown.", "\u16C8"),
        ReadCard("Algiz", "Elk-sedge: protection, higher guidance, a shield raised.",
            "Vulnerability; ignored warning signs, or protection that has lapsed.", "\u16C9"),
        ReadCard("Sowilo", "The sun: success, clarity, life-force and victory.", "", "\u16CA"),
        ReadCard("Tiwaz", "The warrior's spear: justice, courage, sacrifice for a just cause.",
            "Courage failing; unjust conflict, or will without direction.", "\u16CF"),
        ReadCard("Berkano", "Birch: birth, growth, nurturing and new beginnings.",
            "Stunted growth; family strain, or something not yet ready to be born.", "\u16D2"),
        ReadCard("Ehwaz", "The horse: trust, teamwork, steady movement forward together.",
            "Mistrust or a mismatched partnership; a move made too soon.", "\u16D6"),
        ReadCard("Mannaz", "Humankind: the self in community, cooperation, shared intelligence.",
            "Isolation; self-deception, or turning away from those who would help.", "\u16D7"),
        ReadCard("Laguz", "Water: flow, intuition, the unconscious and the tides of feeling.",
            "Fear of the depths; confusion, drained intuition, or feelings held back.", "\u16DA"),
        ReadCard("Ingwaz", "Seed of potential: gestation, rest before release, inner completion.", "", "\u16DC"),
        ReadCard("Dagaz", "Daybreak: breakthrough, transformation, a turning point in awareness.", "", "\u16DE"),
        ReadCard("Othala", "Inheritance: home, ancestry, what is truly yours to keep.",
            "Inheritance withheld; rootlessness, or clinging to what has outlived its purpose.", "\u16DF")
    )

    // ------------------------------------------------------------------ tarot

    private val majors: List<ReadCard> = listOf(
        ReadCard("The Fool", "A fresh start and a leap of faith: innocence, open potential.",
            "Hesitation or recklessness; a leap taken blind, or one not taken."),
        ReadCard("The Magician", "Will and skill aligned: you have the tools to make it happen.",
            "Scattered talent; manipulation, or tools left unused."),
        ReadCard("The High Priestess", "Intuition and the unspoken: trust the quiet knowing.",
            "Ignored instincts; secrets, or distance from your inner voice."),
        ReadCard("The Empress", "Abundance, nurture and creative growth.",
            "Smothering or neglect; a creative block, over-dependence."),
        ReadCard("The Emperor", "Structure, authority and stable foundations.",
            "Rigid control or weak boundaries; power misused."),
        ReadCard("The Hierophant", "Tradition, teaching and shared values.",
            "Convention broken; dogma questioned, a need to find your own way."),
        ReadCard("The Lovers", "Union, and a meaningful choice aligned with your values.",
            "Disharmony or a choice avoided; values out of line."),
        ReadCard("The Chariot", "Determination and control: victory through focus.",
            "Lost direction; opposing forces pulling apart."),
        ReadCard("Strength", "Quiet courage, patience and compassion over force.",
            "Self-doubt or raw temper; courage run low."),
        ReadCard("The Hermit", "Withdrawal for insight: solitude as a search for truth.",
            "Isolation held too long, or avoiding the inner work."),
        ReadCard("Wheel of Fortune", "Cycles turning: change, luck, fate in motion.",
            "Resisting change; a downturn or bad timing that will pass."),
        ReadCard("Justice", "Truth, fairness, cause and effect.",
            "Bias, dishonesty, or consequences dodged."),
        ReadCard("The Hanged Man", "Pause and surrender: a new view through letting go.",
            "Stalling for its own sake; sacrifice without purpose."),
        ReadCard("Death", "An ending that makes way for transformation.",
            "Clinging to what is finished; slow, resisted change."),
        ReadCard("Temperance", "Balance, patience and the blending of opposites.",
            "Excess or imbalance; impatience."),
        ReadCard("The Devil", "Bonds and shadow: attachment, compulsion, temptation.",
            "Chains loosening; reclaiming power, facing the shadow."),
        ReadCard("The Tower", "Sudden upheaval that tears down false structures.",
            "Disaster avoided or delayed; fear of necessary change."),
        ReadCard("The Star", "Hope, healing and renewed faith.",
            "Discouragement; hope worn thin, disconnection."),
        ReadCard("The Moon", "Illusion, dreams and the subconscious: things are not as they seem.",
            "Fog lifting; fears confronted, or confusion that lingers."),
        ReadCard("The Sun", "Joy, clarity and vitality: things going well.",
            "Dimmed joy; temporary clouds, overconfidence."),
        ReadCard("Judgement", "Awakening, reckoning, answering a calling.",
            "Self-doubt; avoiding the reckoning, ignoring the call."),
        ReadCard("The World", "Completion, wholeness, a cycle fulfilled.",
            "Unfinished business; a last step still to take.")
    )

    private val tarotSuits = listOf(
        "Wands" to "action, passion and creative drive",
        "Cups" to "feelings, relationships and intuition",
        "Swords" to "thought, truth and conflict",
        "Pentacles" to "work, money and the material world"
    )

    private val tarotRanks = listOf(
        Triple("Ace", "A new beginning and raw potential in %s", "A missed or delayed start in %s"),
        Triple("Two", "Balance and a choice between two paths in %s", "Imbalance or indecision in %s"),
        Triple("Three", "Growth and early collaboration in %s", "Stalled growth or friction among partners in %s"),
        Triple("Four", "Stability and rest in %s", "Restlessness or stagnation in %s"),
        Triple("Five", "Strain, loss or conflict in %s", "Recovery after strain in %s"),
        Triple("Six", "Harmony restored, moving toward easier ground in %s", "Old patterns dragging at progress in %s"),
        Triple("Seven", "A test of nerve and strategy in %s", "Doubt, evasion, or giving up too soon in %s"),
        Triple("Eight", "Focused effort and momentum in %s", "Scattered effort or burnout in %s"),
        Triple("Nine", "Near-completion, resilience, and what you have built in %s", "Anxiety or isolation just before the finish in %s"),
        Triple("Ten", "A cycle reaching its peak or its limit in %s", "Overload, or release from a burden in %s"),
        Triple("Page", "A curious messenger bringing news or a lesson in %s", "Immaturity, or a message misread in %s"),
        Triple("Knight", "Bold pursuit and action in %s", "Rash haste, or stalling in %s"),
        Triple("Queen", "Mature, intuitive mastery of %s", "Moodiness or withheld support in %s"),
        Triple("King", "Authority and command of %s", "Control misused or lost in %s")
    )

    private val tarotDeck: List<ReadCard> by lazy {
        val minors = tarotSuits.flatMap { (suit, domain) ->
            tarotRanks.map { (rank, up, rev) ->
                ReadCard("$rank of $suit", up.replace("%s", domain) + ".", rev.replace("%s", domain) + ".")
            }
        }
        majors + minors
    }

    // ------------------------------------------------------- playing cards

    private val playingSuits = listOf(
        Triple("Hearts", "\u2665", "love, home and the heart"),
        Triple("Clubs", "\u2663", "work, ambition and friendship"),
        Triple("Diamonds", "\u2666", "money, resources and news"),
        Triple("Spades", "\u2660", "challenge, change and hard truths")
    )

    private val playingRanks = listOf(
        "Ace" to "A beginning, or the heart of the matter, in %s",
        "Two" to "Partnership or a meeting of two in %s",
        "Three" to "A choice, or a third factor entering, in %s",
        "Four" to "Stability and foundations in %s",
        "Five" to "Disruption or a change of plan in %s",
        "Six" to "Cooperation and a smoother stretch in %s",
        "Seven" to "Reflection, waiting, or a hidden snag in %s",
        "Eight" to "Movement and momentum in %s",
        "Nine" to "Fulfilment, the wish card, for %s",
        "Ten" to "Culmination and consequence in %s",
        "Jack" to "A young or helpful person, or news, in %s",
        "Queen" to "A nurturing, perceptive presence in %s",
        "King" to "A mature, authoritative presence in %s"
    )

    private val playingDeck: List<ReadCard> by lazy {
        playingSuits.flatMap { (suit, glyph, domain) ->
            playingRanks.map { (rank, up) ->
                ReadCard("$rank of $suit", up.replace("%s", domain) + ".", "", glyph)
            }
        }
    }
}
