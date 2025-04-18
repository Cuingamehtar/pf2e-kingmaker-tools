package at.posselt.pfrpg2e.camping

import at.posselt.pfrpg2e.camping.dialogs.RegionSetting
import at.posselt.pfrpg2e.data.checks.DegreeOfSuccess
import at.posselt.pfrpg2e.data.checks.RollMode
import at.posselt.pfrpg2e.fromCamelCase
import at.posselt.pfrpg2e.utils.d20Check
import at.posselt.pfrpg2e.utils.fromUuidTypeSafe
import at.posselt.pfrpg2e.utils.getPF2EWorldTime
import at.posselt.pfrpg2e.utils.isDay
import at.posselt.pfrpg2e.utils.rollWithDraw
import at.posselt.pfrpg2e.utils.t
import com.foundryvtt.core.Game
import com.foundryvtt.core.documents.RollTable
import com.foundryvtt.core.ui
import js.objects.recordOf

suspend fun rollRandomEncounter(
    game: Game,
    actor: CampingActor,
    includeFlatCheck: Boolean
): Boolean {
    actor.getCamping()?.let { camping ->
        val currentRegion = camping.findCurrentRegion() ?: camping.regionSettings.regions.firstOrNull()
        currentRegion?.let { region ->
            val partyLevel = camping.getAveragePartyLevel()
            return rollRandomEncounter(
                camping = camping,
                includeFlatCheck = includeFlatCheck,
                region = region,
                isDay = game.getPF2EWorldTime().time.isDay(),
                partyLevel = partyLevel,
            )
        }
    }
    return false
}

private suspend fun rollRandomEncounter(
    camping: CampingData,
    includeFlatCheck: Boolean,
    region: RegionSetting,
    isDay: Boolean,
    partyLevel: Int,
): Boolean {
    val table = region.rollTableUuid?.let { fromUuidTypeSafe<RollTable>(it) }
    if (table == null) {
        if (region.rollTableUuid != null) {
            ui.notifications.error(t("camping.encounterTableNotFound", recordOf("regionName" to region.name)))
        }
        return false
    }
    val rollMode = fromCamelCase<RollMode>(camping.randomEncounterRollMode) ?: RollMode.GMROLL
    val proxyTable = camping.proxyRandomEncounterTableUuid?.let { fromUuidTypeSafe<RollTable>(it) }
    val dc = findEncounterDcModifier(camping, isDay)
    val rollCheck = if (includeFlatCheck) {
        d20Check(
            dc = dc,
            flavor = t("camping.rollingRandomEncounter", recordOf("regionName" to region.name, "dc" to dc)),
            rollMode = rollMode,
        ).degreeOfSuccess.succeeded()
    } else {
        true
    }
    if (rollCheck) {
        val proxyResult = proxyTable?.rollWithDraw(rollMode = rollMode)
            ?.draw
            ?.results
            ?.get(0)
            ?.text
            ?.trim()
            ?: "Creature"
        if (proxyResult == "Creature") {
            table.rollWithDraw(rollMode = rollMode)
        }
        if (camping.campingActivities.any {
                val result = it.parseResult()
                it.isPrepareCampsite() && result != null && result != DegreeOfSuccess.CRITICAL_FAILURE
            }) {
            postCombatEffects(
                activeActivities = camping.alwaysPerformActivityIds.toSet() +
                        camping.campingActivities
                            .filter { it.actorUuid != null }
                            .map { it.activityId },
                partyLevel = partyLevel
            )
        }
        return true
    }
    return false
}

fun findEncounterDcModifier(
    camping: CampingData,
    isDay: Boolean
): Int = (camping.findCurrentRegion()?.encounterDc ?: 0) +
        calculateModifierIncrease(camping, isDay) +
        camping.encounterModifier

private fun calculateModifierIncrease(camping: CampingData, isDay: Boolean): Int =
    camping.groupActivities().asSequence()
        .filter { it.done() || camping.alwaysPerformActivityIds.contains(it.data.id) }
        .map { (data, activity) -> calculateModifierIncrease(data, isDay, activity.parseResult()) }
        .sum()


private fun calculateModifierIncrease(
    data: CampingActivityData,
    isDay: Boolean,
    checkResult: DegreeOfSuccess?
): Int {
    val activityMod = data.modifyRandomEncounterDc?.atTime(isDay) ?: 0
    val resultMod = checkResult?.let { result ->
        data.getOutcome(result)
            ?.modifyRandomEncounterDc
            ?.atTime(isDay)
    } ?: 0
    return activityMod + resultMod
}
