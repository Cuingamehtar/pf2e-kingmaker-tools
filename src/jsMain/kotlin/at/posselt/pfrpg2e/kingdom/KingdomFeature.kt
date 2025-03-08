package at.posselt.pfrpg2e.kingdom

import js.objects.JsPlainObject
import kotlinx.serialization.json.JsonElement


@JsPlainObject
external interface KingdomFeature {
    val id: String
    val levels: Array<Int>
    val name: String
    val description: String
    val flags: Array<String>?
    val modifiers: Array<RawModifier>?
    val freeBoosts: Int?
    val skillProficiencies: Int?
    val abilityBoosts: Int?
    val ruinThresholdIncreases: RawRuinThresholdIncreases?
    val skillIncrease: Boolean?
    val kingdomFeat: Boolean?
}

fun KingdomData.getFeatures(): Array<KingdomFeature> {
    if (settings.kingdomSkillIncreaseEveryLevel) {
        return kingdomFeatures.map {
            if (it.id == "skill-increase") {
                it.copy(levels = (2..20).toList().toTypedArray())
            } else {
                it
            }
        }.toTypedArray()
    } else {
        return kingdomFeatures
    }
}

fun KingdomData.getExplodedFeatures() =
    getFeatures().flatMap { it.explodeLevels() }

fun KingdomFeature.explodeLevels(): List<RawExplodedKingdomFeature> =
    levels.map {
        RawExplodedKingdomFeature(
            id = "$id-level-$it",
            level = it,
            levels = levels,
            name = name,
            description = description,
            flags = flags,
            modifiers = modifiers,
            freeBoosts = freeBoosts,
            skillProficiencies = skillProficiencies,
            abilityBoosts = abilityBoosts,
            ruinThresholdIncreases = ruinThresholdIncreases,
            skillIncrease = skillIncrease,
            kingdomFeat = kingdomFeat,
        )
    }

@JsPlainObject
external interface RawExplodedKingdomFeature : KingdomFeature {
    val level: Int
}


@JsModule("./features.json")
external val kingdomFeatures: Array<KingdomFeature>

@JsModule("./schemas/feature.json")
external val kingdomFeatureSchema: JsonElement