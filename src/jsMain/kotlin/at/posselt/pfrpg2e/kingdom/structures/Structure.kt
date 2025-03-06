package at.posselt.pfrpg2e.kingdom.structures

import at.posselt.pfrpg2e.data.kingdom.KingdomSkill
import at.posselt.pfrpg2e.data.kingdom.KingdomSkillRank
import at.posselt.pfrpg2e.data.kingdom.Ruin
import at.posselt.pfrpg2e.data.kingdom.structures.AvailableItemsRule
import at.posselt.pfrpg2e.data.kingdom.structures.CommodityStorage
import at.posselt.pfrpg2e.data.kingdom.structures.Construction
import at.posselt.pfrpg2e.data.kingdom.structures.IncreaseResourceDice
import at.posselt.pfrpg2e.data.kingdom.structures.ItemGroup
import at.posselt.pfrpg2e.data.kingdom.structures.ReduceUnrestBy
import at.posselt.pfrpg2e.data.kingdom.structures.RuinAmount
import at.posselt.pfrpg2e.data.kingdom.structures.Structure
import at.posselt.pfrpg2e.data.kingdom.structures.StructureBonus
import at.posselt.pfrpg2e.data.kingdom.structures.StructureTrait
import at.posselt.pfrpg2e.kingdom.getRawStructureData
import at.posselt.pfrpg2e.utils.asAnyObject
import com.foundryvtt.core.AnyObject
import com.foundryvtt.pf2e.actor.PF2ENpc
import kotlin.contracts.contract


data class ActorAndStructure(
    val actor: PF2ENpc,
    val structure: RawStructureData,
)

@Suppress(
    "NOTHING_TO_INLINE",
    "CANNOT_CHECK_FOR_EXTERNAL_INTERFACE",
    "CANNOT_CHECK_FOR_ERASED",
    "ERROR_IN_CONTRACT_DESCRIPTION"
)
private fun isStructureRef(obj: AnyObject): Boolean {
    contract {
        returns(true) implies (obj is StructureRef)
    }
    return obj["ref"] is String
}

class StructureParsingException(message: String) : Exception(message)

fun PF2ENpc.getRawResolvedStructureData(): RawStructureData? {
    val data = getRawStructureData()
    if (data == null) return null
    val record = data.asAnyObject()
    return if (isStructureRef(record)) {
        structures.find { it.name == record.ref }
            ?: throw StructureParsingException("Could not find existing structure with ref ${record.ref}")
    } else {
        data.unsafeCast<RawStructureData>()
    }
    return null
}

fun PF2ENpc.isStructure() = getRawStructureData() != null

fun PF2ENpc.parseStructure(): Structure? = getRawResolvedStructureData()?.parseStructure()

fun RawStructureData.parseStructure() =
    Structure(
        name = name,
        stacksWith = stacksWith,
        construction = Construction(
            skills = construction?.skills?.mapNotNull {
                KingdomSkill.fromString(it.skill)?.let { skill ->
                    KingdomSkillRank(
                        skill = skill,
                        rank = it.proficiencyRank ?: 0,
                    )
                }
            }?.toSet() ?: emptySet(),
            lumber = construction?.lumber ?: 0,
            luxuries = construction?.luxuries ?: 0,
            ore = construction?.ore ?: 0,
            stone = construction?.stone ?: 0,
            rp = construction?.rp ?: 0,
            dc = construction?.dc ?: 0,
        ),
        notes = notes,
        preventItemLevelPenalty = preventItemLevelPenalty == true,
        enableCapitalInvestment = enableCapitalInvestment == true,
        bonuses = (skillBonusRules?.mapNotNull { rule ->
            KingdomSkill.fromString(rule.skill)?.let { skill ->
                StructureBonus(
                    skill = skill,
                    activity = rule.activity,
                    value = rule.value,
                )
            }
        }?.toSet() ?: emptySet()) + (activityBonusRules?.mapNotNull { rule ->
            StructureBonus(
                activity = rule.activity,
                value = rule.value,
                skill = null,
            )
        }?.toSet() ?: emptySet()),
        availableItemsRules = availableItemsRules?.mapNotNull { rule ->
            val group = rule.group?.let { group -> ItemGroup.fromString(group) }
            AvailableItemsRule(
                value = rule.value,
                group = group,
                maximumStacks = rule.maximumStacks,
            )
        }?.toSet() ?: emptySet(),
        settlementEventBonus = settlementEventRules?.firstOrNull()?.value ?: 0,
        leadershipActivityBonus = leadershipActivityRules?.firstOrNull()?.value ?: 0,
        storage = CommodityStorage(
            ore = storage?.ore ?: 0,
            food = storage?.food ?: 0,
            lumber = storage?.lumber ?: 0,
            stone = storage?.stone ?: 0,
            luxuries = storage?.luxuries ?: 0,
        ),
        increaseLeadershipActivities = increaseLeadershipActivities == true,
        isBridge = isBridge == true,
        consumptionReduction = consumptionReduction ?: 0,
        unlockActivities = unlockActivities?.toSet() ?: emptySet(),
        traits = traits?.mapNotNull { StructureTrait.fromString(it) }?.toSet() ?: emptySet(),
        lots = lots,
        affectsEvents = affectsEvents == true,
        affectsDowntime = affectsDowntime == true,
        reducesUnrest = reducesUnrest == true,
        reducesRuin = reducesRuin == true,
        level = level,
        upgradeFrom = upgradeFrom?.toSet() ?: emptySet(),
        reduceUnrestBy = reduceUnrestBy?.let { unrest ->
            ReduceUnrestBy(
                value = unrest.value,
                moreThanOncePerTurn = unrest.moreThanOncePerTurn == true,
                note = unrest.note,
            )
        },
        reduceRuinBy = reduceRuinBy?.let { ruin ->
            RuinAmount(
                value = ruin.value,
                ruin = Ruin.fromString(ruin.ruin) ?: Ruin.CRIME,
                moreThanOncePerTurn = ruin.moreThanOncePerTurn == true,
            )
        },
        gainRuin = gainRuin?.let { ruin ->
            RuinAmount(
                value = ruin.value,
                ruin = Ruin.fromString(ruin.ruin) ?: Ruin.CRIME,
                moreThanOncePerTurn = ruin.moreThanOncePerTurn == true,
            )
        },
        increaseResourceDice = IncreaseResourceDice(
            village = increaseResourceDice?.village ?: 0,
            town = increaseResourceDice?.town ?: 0,
            city = increaseResourceDice?.city ?: 0,
            metropolis = increaseResourceDice?.metropolis ?: 0,
        ),
        consumptionReductionStacks = consumptionReductionStacks == true,
        ignoreConsumptionReductionOf = ignoreConsumptionReductionOf?.toSet() ?: emptySet(),
    )

fun PF2ENpc.getActorAndStructure(): ActorAndStructure? {
    val data = getRawResolvedStructureData()
    return data?.let {
        ActorAndStructure(
            actor = this,
            structure = it,
        )
    }
}

