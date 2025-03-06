package at.posselt.pfrpg2e.kingdom.modifiers.expressions

import at.posselt.pfrpg2e.data.kingdom.KingdomPhase
import at.posselt.pfrpg2e.data.kingdom.KingdomSkill
import at.posselt.pfrpg2e.data.kingdom.KingdomSkillRanks
import at.posselt.pfrpg2e.data.kingdom.leaders.Leader
import at.posselt.pfrpg2e.data.kingdom.structures.Structure
import at.posselt.pfrpg2e.toCamelCase

data class ExpressionContext(
    val usedSkill: KingdomSkill,
    val ranks: KingdomSkillRanks,
    val leader: Leader,
    val activity: String?,
    val phase: KingdomPhase?,
    val level: Int,
    val unrest: Int,
    val flags: Set<String>,
    val rollOptions: Set<String>,
    val isVacant: Boolean,
    val structure: Structure?
) {
    fun evaluateBool(expression: Any?): Boolean {
        val result = evaluateExpression(expression)
        return parseBooleanOrFalse(result, expression)
    }

    fun evaluateInt(expression: Any?): Int {
        val result = evaluateExpression(expression)
        return parseIntOr0(result)
    }

    fun evaluateExpression(expression: Any?): Any? {
        return when (expression) {
            "@ability" -> usedSkill.ability.value
            "@leader" -> leader.value
            "@phase" -> phase?.value
            "@structure" -> structure?.name
            "@activity" -> activity
            "@unrest" -> unrest
            "@vacant" -> isVacant
            "@agricultureRank" -> ranks.agriculture
            "@artsRank" -> ranks.arts
            "@boatingRank" -> ranks.boating
            "@defenseRank" -> ranks.defense
            "@engineeringRank" -> ranks.engineering
            "@explorationRank" -> ranks.exploration
            "@folkloreRank" -> ranks.folklore
            "@industryRank" -> ranks.industry
            "@intrigueRank" -> ranks.intrigue
            "@magicRank" -> ranks.magic
            "@politicsRank" -> ranks.politics
            "@scholarshipRank" -> ranks.scholarship
            "@statecraftRank" -> ranks.statecraft
            "@tradeRank" -> ranks.trade
            "@warfareRank" -> ranks.warfare
            "@wildernessRank" -> ranks.wilderness
            "@kingdomLevel" -> level
            "@skillRank" -> ranks.resolve(usedSkill)
            "@skill" -> usedSkill.toCamelCase()
            else -> expression
        }
    }
}

fun parseIntOr0(result: Any?): Int {
    return when (result) {
        is Int -> result
        is Boolean -> if (result) 1 else 0
        is String -> try {
            result.toInt()
        } catch (_: NumberFormatException) {
            console.error("Predicate Evaluation: Could not turn $result to int")
            0
        }

        else -> 0
    }
}

fun parseBooleanOrFalse(result: Any?, expression: Any?): Boolean {
    return when (result) {
        is Int -> expression != 0
        is Boolean -> result
        is String -> result == "true"
        else -> false
    }
}