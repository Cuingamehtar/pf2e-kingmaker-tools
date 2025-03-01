package at.posselt.pfrpg2e.data.actor

import at.posselt.pfrpg2e.fromCamelCase
import at.posselt.pfrpg2e.toCamelCase
import at.posselt.pfrpg2e.toLabel

enum class Proficiency {
    UNTRAINED,
    TRAINED,
    EXPERT,
    MASTER,
    LEGENDARY;

    companion object {
        fun fromString(value: String) = fromCamelCase<Proficiency>(value)
    }

    val value: String
        get() = toCamelCase()

    val label: String
        get() = toLabel()
}