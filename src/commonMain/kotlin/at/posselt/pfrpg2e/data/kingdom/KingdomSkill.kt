package at.posselt.pfrpg2e.data.kingdom

import at.posselt.pfrpg2e.fromCamelCase
import at.posselt.pfrpg2e.toCamelCase
import at.posselt.pfrpg2e.toLabel


enum class KingdomSkill(val ability: KingdomAbility) {
    AGRICULTURE(KingdomAbility.STABILITY),
    ARTS(KingdomAbility.CULTURE),
    BOATING(KingdomAbility.ECONOMY),
    DEFENSE(KingdomAbility.STABILITY),
    ENGINEERING(KingdomAbility.STABILITY),
    EXPLORATION(KingdomAbility.ECONOMY),
    FOLKLORE(KingdomAbility.CULTURE),
    INDUSTRY(KingdomAbility.ECONOMY),
    INTRIGUE(KingdomAbility.LOYALTY),
    MAGIC(KingdomAbility.CULTURE),
    POLITICS(KingdomAbility.LOYALTY),
    SCHOLARSHIP(KingdomAbility.CULTURE),
    STATECRAFT(KingdomAbility.LOYALTY),
    TRADE(KingdomAbility.ECONOMY),
    WARFARE(KingdomAbility.LOYALTY),
    WILDERNESS(KingdomAbility.STABILITY);

    companion object {
        fun fromString(value: String) = fromCamelCase<KingdomSkill>(value)
    }

    val value: String
        get() = toCamelCase()

    val label: String
        get() = toLabel()
}