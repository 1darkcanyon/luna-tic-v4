package net.kaneonexus.lunatic

import kotlin.random.Random

/**
 * Shared reading engine for LUNA-TIC.
 * One draw/spread system serves every deck, so a new deck only needs a card list
 * and a spread list. All meanings are original text written for LUNA-TIC.
 */
object ReadingEngine {

    enum class Kind(val label: String) {
        RUNES("Runes"),
        TAROT("Tarot"),
        DARKDAYS("Dark Days"),
        DEVIANT("Deviant Moon"),
        CARDS("Playing cards")
    }

    /** reversed == "" means the card cannot be reversed (symmetrical runes, playing cards). */
    data class ReadCard(
        val name: String,
        val upright: String,
        val reversed: String = "",
        val glyph: String = ""
    ) {
        val canReverse: Boolean get() = reversed.isNotEmpty()
    }

    data class Spread(val id: String, val name: String, val positions: List<String>)

    /** Dark Days reads a card four ways, tied to the moon's cycle. */
    data class Orient(val name: String, val phase: String, val line: String)

    val moonOrients: List<Orient> = listOf(
        Orient("Upright", "Dark moon \u00B7 North \u00B7 Winter \u00B7 Earth",
            "A quiet, inward message. Slow down, ground yourself, and let it settle before you act."),
        Orient("Right-pointing", "Waxing moon \u00B7 East \u00B7 Spring \u00B7 Air",
            "A message of beginnings and fresh thinking. Something is gathering; give it clarity and attention."),
        Orient("Reversed", "Full moon \u00B7 South \u00B7 Summer \u00B7 Fire",
            "A message at full strength: lit up and hard to ignore. Look at its shadow side too."),
        Orient("Left-pointing", "Waning moon \u00B7 West \u00B7 Autumn \u00B7 Water",
            "A message of release. Let feeling move, and let go of what has done its work.")
    )

    data class Drawn(
        val position: String,
        val card: ReadCard,
        val isReversed: Boolean,
        val orient: Int = -1 // index into moonOrients, or -1 when not used
    ) {
        val meaning: String
            get() {
                if (orient >= 0) {
                    val o = moonOrients[orient]
                    val shadow = if (orient == 2 && card.reversed.isNotEmpty()) "\nShadow side: ${card.reversed}" else ""
                    return "${o.phase}\n${o.line}\n\n${card.upright}$shadow"
                }
                return if (isReversed) card.reversed else card.upright
            }

        val label: String
            get() {
                if (orient >= 0) return "${card.name} \u2014 ${moonOrients[orient].name}"
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

    private val celticPositions = listOf(
        "The present", "The challenge", "The foundation", "The recent past",
        "The crown (best outcome)", "The near future", "The self",
        "Surroundings", "Hopes and fears", "The outcome"
    )

    fun spreads(kind: Kind): List<Spread> = when (kind) {
        Kind.RUNES -> listOf(
            Spread("r1", "One rune", listOf("Guidance")),
            Spread("r3", "Three runes", listOf("Past", "Present", "Future")),
            Spread("r5", "Five runes", listOf("Situation", "Challenge", "Hidden influence", "Advice", "Outcome"))
        )
        Kind.TAROT -> listOf(
            Spread("t1", "One card", listOf("The card")),
            Spread("t3", "Three cards", listOf("Past", "Present", "Future")),
            Spread("tcc", "Celtic Cross", celticPositions)
        )
        Kind.DARKDAYS -> listOf(
            Spread("d1", "One card", listOf("The card")),
            Spread("d3", "Three cards", listOf("Past", "Present", "Future")),
            Spread("dm4", "Moon cycle", listOf(
                "Dark moon: what is hidden",
                "Waxing: what is growing",
                "Full: what is revealed",
                "Waning: what to release"
            )),
            Spread("dcc", "Celtic Cross", celticPositions)
        )
        Kind.DEVIANT -> listOf(
            Spread("v1", "One card", listOf("The card")),
            Spread("v3", "Three cards", listOf("Past", "Present", "Future")),
            Spread("vmc", "Moon circle", listOf(
                "You, right now", "Past influences", "Subconscious currents",
                "Secret wishes", "Hidden forces", "What is coming",
                "Your surroundings", "Other people", "Spiritual currents", "The outcome"
            )),
            Spread("vcc", "Celtic Cross", celticPositions)
        )
        Kind.CARDS -> listOf(
            Spread("c1", "One card", listOf("The card")),
            Spread("c3", "Three cards", listOf("Past", "Present", "Future")),
            Spread("c9", "Nine-card square", listOf(
                "Roots", "Recent past", "Fading influence",
                "The present", "Heart of the matter", "Hidden factor",
                "Coming soon", "Advice", "Outcome"
            ))
        )
    }

    fun deck(kind: Kind): List<ReadCard> = when (kind) {
        Kind.RUNES -> runeDeck
        Kind.TAROT -> tarotDeck
        Kind.DARKDAYS -> darkDaysDeck
        Kind.DEVIANT -> deviantDeck
        Kind.CARDS -> playingDeck
    }

    /** Label for the reversal / orientation toggle, or null if the deck has none. */
    fun orientationLabel(kind: Kind): String? = when (kind) {
        Kind.CARDS -> null
        Kind.DARKDAYS -> "Moon orientation"
        else -> "Reversals"
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
            if (kind == Kind.DARKDAYS && allowReversed) {
                Drawn(pos, c, false, rng.nextInt(4))
            } else {
                val rev = allowReversed && c.canReverse && rng.nextBoolean()
                Drawn(pos, c, rev)
            }
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

    // ------------------------------------------------------ shared tarot tools

    private fun buildMinors(
        suits: List<Pair<String, String>>,
        ranks: List<Triple<String, String, String>>,
        overrides: Map<String, Pair<String, String>>
    ): List<ReadCard> = suits.flatMap { (suit, domain) ->
        ranks.map { (rank, up, rev) ->
            val name = "$rank of $suit"
            val o = overrides[name]
            if (o != null) {
                ReadCard(name, o.first, o.second)
            } else {
                ReadCard(name, up.replace("%s", domain) + ".", rev.replace("%s", domain) + ".")
            }
        }
    }

    // ------------------------------------------------------------ standard tarot

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
        majors + buildMinors(tarotSuits, tarotRanks, emptyMap())
    }

    // -------------------------------------------------------------- Dark Days

    private val darkDaysMajors: List<ReadCard> = listOf(
        ReadCard("The Fool", "Stepping into the unknown with light-hearted trust: play, wonder, an open road.",
            "Naivety or avoidance; a leap made without looking, or fear of the first step."),
        ReadCard("The Magician", "Will meets spirit: you can shape what you want most if you picture it clearly.",
            "Talent scattered or used to mislead; a plan that never leaves the mind."),
        ReadCard("The High Priestess", "The hidden knowing of the subconscious: dreams, ancestry, a message from below the surface.",
            "Intuition ignored or muddied; secrets kept even from yourself."),
        ReadCard("The Empress", "Fertile abundance: the body, the garden, creative life growing in its own time.",
            "Overgiving or depletion; creative ground left fallow, or care withheld."),
        ReadCard("The Emperor", "Order and protection: a structure strong enough to hold what you are building.",
            "Rigidity or a crumbling structure; authority used without care."),
        ReadCard("The Hierophant", "Inherited wisdom and ritual: learning from a lineage or a teacher you trust.",
            "Dogma outgrown; time to build a practice of your own."),
        ReadCard("The Lovers", "Union and the heart's true choice: desire and values pointing the same way.",
            "Divided loyalties; a choice avoided, or closeness out of balance."),
        ReadCard("The Chariot", "Will and momentum: opposing forces steered toward a single aim.",
            "Losing the reins; drive with no direction."),
        ReadCard("Strength", "Gentle power: taming the inner beast with patience, not force.",
            "Doubt or swallowed fury; strength spent against yourself."),
        ReadCard("The Hermit", "The lantern in the dark: retreat that brings clear inner guidance.",
            "Withdrawal turned to loneliness; refusing the light you carry."),
        ReadCard("Wheel of Fortune", "The cycle turns: luck, timing and the pull of larger rhythms.",
            "Resisting the turning; bad timing that will pass."),
        ReadCard("Justice", "Truth weighed fairly: an honest accounting, consequences that make sense.",
            "Skewed judgment; dodging accountability."),
        ReadCard("The Hanged Man", "Suspension and surrender: a changed view from letting the moment hold you.",
            "Stuck for the sake of it; a sacrifice nobody asked for."),
        ReadCard("Death", "A natural ending, the dark moon before renewal: release makes room.",
            "Holding on to what is already gone; transition dragged out by fear."),
        ReadCard("Temperance", "Gentle blending: patience and the middle way between extremes.",
            "Imbalance or impatience; ingredients that do not yet mix."),
        ReadCard("The Devil", "Shadow and attachment: what binds you is also showing you your power.",
            "Chains loosening; a bond recognized and ready to be released."),
        ReadCard("The Tower", "A sudden collapse of what was built on false ground: lightning that frees.",
            "Disruption resisted or delayed; fear of the necessary fall."),
        ReadCard("The Star", "Quiet hope and healing: faith renewed under open sky.",
            "Hope dimmed; disconnection from what restores you."),
        ReadCard("The Moon", "Dreams, tides and illusion: trust what you feel, but check what is real.",
            "Fog thinning; fears named, or illusion deepening."),
        ReadCard("The Sun", "Plain joy, clarity and vitality: things are warm and visible.",
            "Joy muted; clouds passing, or certainty too bright to question."),
        ReadCard("Judgement", "A calling heard: reckoning, forgiveness and rising renewed.",
            "Self-judgment, or ignoring the call; a reckoning put off."),
        ReadCard("The World", "A cycle complete and whole: integration, with the next circle already forming.",
            "A last loose end; completion just out of reach.")
    )

    private val darkDaysSuits = listOf(
        "Wands" to "will, passion and creative fire",
        "Swords" to "thought, clarity and honest speech",
        "Cups" to "emotion and the heart's tides",
        "Pentacles" to "the body, work and resources"
    )

    private val darkDaysRanks = listOf(
        Triple("Ace", "Independence, will and a clear new purpose in %s", "A purpose still unformed, or will held back in %s"),
        Triple("Two", "Balance, contrast and a choice between two in %s", "Pulled between opposites in %s"),
        Triple("Three", "Fertile collaboration and growth in %s", "Growth stalled or crowded in %s"),
        Triple("Four", "Stability and endurance in %s", "Rigidity or stagnation in %s"),
        Triple("Five", "Erratic motion and a shifting self-image in %s", "Steadying after upheaval in %s"),
        Triple("Six", "Sincerity and gentle movement in %s", "Insincerity, or a move that will not come in %s"),
        Triple("Seven", "Conflict that leads to understanding in %s", "Avoiding the conflict, or understanding withheld in %s"),
        Triple("Eight", "Opportunity and repeated, intentional effort in %s", "A missed opening, or empty repetition in %s"),
        Triple("Nine", "Vision and attainment, and the influence they bring, in %s", "Vision clouded, or attainment just out of reach in %s"),
        Triple("Ten", "A threshold from an ending into a beginning in %s", "An ending resisted in %s"),
        Triple("Page", "A messenger or muse arriving in %s", "A message misheard, or a muse ignored in %s"),
        Triple("Knight", "Help, sharp focus and decisive change in %s", "Haste or misdirected action in %s"),
        Triple("Queen", "Compassionate, sustaining mastery of %s", "Mastery turned inward or withheld in %s"),
        Triple("King", "Mature, brave command of %s", "Control hardened into domination in %s")
    )

    private val darkDaysDeck: List<ReadCard> by lazy {
        darkDaysMajors + buildMinors(darkDaysSuits, darkDaysRanks, emptyMap())
    }

    // ------------------------------------------------------------ Deviant Moon

    private val deviantMajors: List<ReadCard> = listOf(
        ReadCard("The Fool", "Wandering into the unknown without fear because without experience: innocence, folly, raw potential.",
            "Unsure steps; the moment missed, or fear arriving too late."),
        ReadCard("The Magician", "A natural gift put to work: emerging skill and clever hands.",
            "Talent misapplied; confusion and scattered effort."),
        ReadCard("The High Priestess", "Guarded wisdom earned through hardship: deep intuition, a keeper of what she knows.",
            "Willful ignorance; feelings held back, vision clouded."),
        ReadCard("The Empress", "Fertility and the authority of nature: a strong maternal force.",
            "Strife at home, or a creative drought; trust broken."),
        ReadCard("The Emperor", "Command and ambition: achievement enjoyed while the next conquest is already in sight.",
            "Weakness at the top; indecision, a ruler out of his depth."),
        ReadCard("The Hierophant", "Faith as comfort and as control: counsel, mercy, conformity.",
            "Unorthodox paths; poor advice, hollow authority."),
        ReadCard("The Lovers", "Passion held in a hypnotic trance: love with a sting in it.",
            "Separation and division; the spell breaking."),
        ReadCard("The Chariot", "Leaving the city stronger for past hardship: a bold journey into new ground.",
            "Plans collapsing; a poor strategy."),
        ReadCard("Strength", "Will pressed on a wild thing: endurance, with the risk that the beast prevails.",
            "Defeat, or surrender to the thing you tried to control."),
        ReadCard("The Hermit", "Retreat into a private alcove: solitude and secrecy, never quite alone with yourself.",
            "Rejoining the crowd; sharing what was hidden."),
        ReadCard("Wheel of Fortune", "Fate spun by an indifferent hand: chance and unplanned events.",
            "Unlucky turns and unexpected results."),
        ReadCard("Justice", "Balance and fairness, in a city where corruption seeps through the seams.",
            "Bias, abuse of the scales, taking sides."),
        ReadCard("The Hanged Man", "Dangling in limbo: indecision while time keeps moving.",
            "Time wasted; feeling stalled in life."),
        ReadCard("Death", "Endings and rebirth entwined: loss and metamorphosis.",
            "Violent, unsettled change; a hard stretch."),
        ReadCard("Temperance", "Patient blending of unlike things into harmony.",
            "Discord, excess, mixtures that go wrong."),
        ReadCard("The Devil", "Obsessive desire and bondage: material chains and dark fantasies.",
            "Greed and relentless acquiring; control over another mind."),
        ReadCard("The Tower", "The lightning strikes: ruin, sudden upheaval, trauma.",
            "Unable to break free of sorrow; ruin that keeps repeating."),
        ReadCard("The Star", "Hope shining over bleak places: renewed faith.",
            "Doubting your own gifts; creativity gone stagnant."),
        ReadCard("The Moon", "The deviant moon pulls the strings: illusion, trickery, control working below awareness.",
            "Avoiding reality; delusion, lies and despair."),
        ReadCard("The Sun", "Two made one under a shared light: friendship, joy, a happy bond.",
            "Loneliness; unstable bonds and parting."),
        ReadCard("Judgement", "The dead rise: accountability and awakening.",
            "Dread of the reckoning; guilt, worry and delay."),
        ReadCard("The World", "A long road home that leaves you changed: victory earned the slow way, a cycle complete.",
            "Frustration; a cycle left unfinished.")
    )

    private val deviantSuits = listOf(
        "Swords" to "conflict, secrets and sorrow",
        "Wands" to "creative fire and daring",
        "Cups" to "love, dreams and the sea",
        "Pentacles" to "labor, wealth and what money costs"
    )

    private val deviantOverrides: Map<String, Pair<String, String>> = mapOf(
        "Four of Swords" to ("Rest and retreat underground: dreaming while energy slowly returns." to
            "Rising back into daily life with renewed vigor."),
        "Five of Swords" to ("A hollow, dishonorable win: someone enjoying another's loss." to
            "A false friend exposed; the odds turning."),
        "Seven of Swords" to ("A showy plan with a hidden flaw: the rope is already fraying." to
            "A plan well thought through; sound advice."),
        "Eight of Swords" to ("Bound by a trance and by outside control: restriction and censorship." to
            "Waking up: freedom and independent thought."),
        "Ten of Swords" to ("Total ruin, the shelter turned coffin: unbearable sorrow." to
            "A pause in the calamity; a slight edge over hardship."),
        "Six of Wands" to ("A transformation completed: emerging better than before." to
            "A change not yet adapted to; delay."),
        "Nine of Wands" to ("A pause inside the maze: gathering the will to keep going." to
            "Giving up; obstacles that feel insurmountable."),
        "Two of Cups" to ("A pact of love sealed under a dark sky: partnership, harmony." to
            "Love that is false or broken; interests at odds."),
        "Seven of Cups" to ("Imagination and daydreams: fantasy rendered as art." to
            "Hard facts and realistic goals."),
        "Ten of Pentacles" to ("Family wealth and values handed down, while something is quietly stolen." to
            "Loss of home or money; family trouble.")
    )

    private val deviantDeck: List<ReadCard> by lazy {
        deviantMajors + buildMinors(deviantSuits, tarotRanks, deviantOverrides)
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
