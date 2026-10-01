package net.kaneonexus.lunatic

import kotlin.random.Random

object TarotEngine {

    data class Card(val name: String, val arcana: String, val keyword: String)

    private val majorArcana = listOf(
        Card("The Fool", "Major", "New beginnings, leap of faith"),
        Card("The Magician", "Major", "Manifestation, resourcefulness"),
        Card("The High Priestess", "Major", "Intuition, the unseen"),
        Card("The Empress", "Major", "Abundance, nurturing"),
        Card("The Emperor", "Major", "Structure, authority"),
        Card("The Hierophant", "Major", "Tradition, teaching"),
        Card("The Lovers", "Major", "Union, choice"),
        Card("The Chariot", "Major", "Willpower, direction"),
        Card("Strength", "Major", "Inner courage, patience"),
        Card("The Hermit", "Major", "Introspection, solitude"),
        Card("Wheel of Fortune", "Major", "Cycles, change"),
        Card("Justice", "Major", "Balance, truth"),
        Card("The Hanged Man", "Major", "Surrender, new perspective"),
        Card("Death", "Major", "Transformation, endings"),
        Card("Temperance", "Major", "Alchemy, moderation"),
        Card("The Devil", "Major", "Bondage, shadow"),
        Card("The Tower", "Major", "Sudden change, revelation"),
        Card("The Star", "Major", "Hope, renewal"),
        Card("The Moon", "Major", "Illusion, subconscious"),
        Card("The Sun", "Major", "Joy, vitality"),
        Card("Judgement", "Major", "Awakening, reckoning"),
        Card("The World", "Major", "Completion, wholeness")
    )

    private val suits = listOf("Wands", "Cups", "Swords", "Pentacles")
    private val ranks = listOf(
        "Ace", "Two", "Three", "Four", "Five", "Six", "Seven",
        "Eight", "Nine", "Ten", "Page", "Knight", "Queen", "King"
    )

    private val fullDeck: List<Card> by lazy {
        val minor = suits.flatMap { suit ->
            ranks.map { rank -> Card("$rank of $suit", "Minor", suitKeyword(suit)) }
        }
        majorArcana + minor
    }

    private fun suitKeyword(suit: String) = when (suit) {
        "Wands" -> "Action, passion, creativity"
        "Cups" -> "Emotion, relationships, intuition"
        "Swords" -> "Thought, conflict, clarity"
        "Pentacles" -> "Material, resources, work"
        else -> ""
    }

    fun drawDaily(seed: Long): Card {
        // Deterministic per-day draw so the same day always returns the same card
        val rng = Random(seed)
        return fullDeck[rng.nextInt(fullDeck.size)]
    }

    fun drawRandom(): Card = fullDeck[Random.nextInt(fullDeck.size)]
}
