package at.posselt.pfrpg2e.camping

import at.posselt.pfrpg2e.actor.getEffectNames
import at.posselt.pfrpg2e.camping.dialogs.ActivityEffectTarget
import at.posselt.pfrpg2e.data.checks.DegreeOfSuccess
import at.posselt.pfrpg2e.fromCamelCase
import at.posselt.pfrpg2e.utils.awaitAll
import at.posselt.pfrpg2e.utils.buildPromise
import at.posselt.pfrpg2e.utils.fromUuidTypeSafe
import com.foundryvtt.pf2e.actor.PF2EActor
import com.foundryvtt.pf2e.item.PF2EEffect
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


private data class EffectAndTarget(
    val activityId: String,
    val effect: PF2EEffect,
    val target: ActivityEffectTarget,
    val degreeOfSuccess: DegreeOfSuccess?,
)

private data class NullableEffectAndTarget(
    val activityId: String,
    val effect: PF2EEffect?,
    val target: ActivityEffectTarget,
    val degreeOfSuccess: DegreeOfSuccess?,
) {
    fun validate(): EffectAndTarget? =
        if (effect == null) {
            null
        } else {
            EffectAndTarget(
                activityId = activityId,
                effect = effect,
                target = target,
                degreeOfSuccess = degreeOfSuccess,
            )
        }
}

private suspend fun parseActivityEffect(
    activityId: String,
    degreeOfSuccess: DegreeOfSuccess?,
    effect: ActivityEffect,
) = NullableEffectAndTarget(
    effect = fromUuidTypeSafe<PF2EEffect>(effect.uuid),
    target = effect.target?.let { fromCamelCase<ActivityEffectTarget>(it) } ?: ActivityEffectTarget.ALL,
    degreeOfSuccess = degreeOfSuccess,
    activityId = activityId,
)

private suspend fun parseActivityEffects(
    activityId: String,
    degreeOfSuccess: DegreeOfSuccess?,
    effects: Array<ActivityEffect>?,
): List<Deferred<NullableEffectAndTarget>> = coroutineScope {
    effects?.map { async { parseActivityEffect(activityId, degreeOfSuccess, it) } }
        ?: emptyList()
}


private suspend fun CampingData.getCampingEffectItems(): List<EffectAndTarget> {
    return getAllActivities()
        .flatMap {
            parseActivityEffects(it.id, null, it.effectUuids) +
                    parseActivityEffects(it.id, DegreeOfSuccess.CRITICAL_FAILURE, it.criticalFailure?.effectUuids) +
                    parseActivityEffects(it.id, DegreeOfSuccess.FAILURE, it.failure?.effectUuids) +
                    parseActivityEffects(it.id, DegreeOfSuccess.SUCCESS, it.success?.effectUuids) +
                    parseActivityEffects(it.id, DegreeOfSuccess.CRITICAL_SUCCESS, it.criticalSuccess?.effectUuids)
        }
        .awaitAll()
        .mapNotNull(NullableEffectAndTarget::validate)
}

private fun PF2EActor.findCampingEffectsInInventory(compendiumItems: List<PF2EEffect>): List<PF2EEffect> {
    val names = compendiumItems.map { it.name }.toSet()
    return itemTypes.effect.filter { it.name in names }
}

suspend fun CampingData.syncCampingEffects(activities: Array<CampingActivity>) = coroutineScope {
    val actors = getActorsInCamp(campingActivityOnly = true)
    val allEffects = getCampingEffectItems()
    val activitiesById = activities.associateBy { it.activityId }
    val allRelevantEffectsById = allEffects
        .filter { effect ->
            val activity = activitiesById[effect.activityId]
            if (activity != null) {
                val result = activity.result?.let { fromCamelCase<DegreeOfSuccess>(it) }
                effect.degreeOfSuccess == null || effect.degreeOfSuccess == result
            } else {
                false
            }
        }
        .groupBy { it.activityId }
    val compendiumItems = allEffects.map { it.effect }
    actors.flatMap { actor ->
        val existingEffects = actor.findCampingEffectsInInventory(compendiumItems)
        val syncEffects = activities
            .flatMap { activity ->
                allRelevantEffectsById[activity.activityId]?.asSequence()
                    ?.filter { actorTargetedByActivityEffect(it.target, activity.actorUuid, actor.uuid) }
                    ?.map { it.effect }
                    ?.toList()
                    ?: emptyList()
            }
        val syncNames = syncEffects.map { it.name }.toSet()
        val existingNames = existingEffects.map { it.name }.toSet()
        val removeIds = existingEffects
            .filter { it.name !in syncNames }
            .mapNotNull { it.id }
            .toTypedArray()
        val addEffects = syncEffects
            .filter { it.name !in existingNames }
            .map(PF2EEffect::toObject)
            .toTypedArray()
        listOf(
            async {
                actor.createEmbeddedDocuments<PF2EEffect>("Item", addEffects).await()
                actor.deleteEmbeddedDocuments<PF2EEffect>("Item", removeIds).await()
            },
        )
    }.awaitAll()
}

private fun actorTargetedByActivityEffect(
    target: ActivityEffectTarget,
    activityActorUuid: String?,
    actorUuid: String,
) = target == ActivityEffectTarget.ALL
        || (target == ActivityEffectTarget.SELF && activityActorUuid == actorUuid)
        || (target == ActivityEffectTarget.ALLIES && activityActorUuid != actorUuid)

suspend fun PF2EActor.getAppliedCampingEffects(campingData: List<ActivityEffect>): List<ActivityEffect> {
    val effectNames = getEffectNames()
    return campingData
        .map { buildPromise { fromUuidTypeSafe<PF2EEffect>(it.uuid)?.name to it } }
        .awaitAll()
        .filter { it.first != null && it.first in effectNames }
        .map { it.second }
}

/**
 * Only checks if the top level effect doubles healing
 */
fun campingActivitiesDoublingHealing(campingData: List<CampingActivityData>): List<ActivityEffect> =
    campingActivitiesHaving(campingData) {
        it.doublesHealing == true
    }


private fun campingActivitiesHaving(
    data: List<CampingActivityData>,
    predicate: (ActivityEffect) -> Boolean
): List<ActivityEffect> =
    data.asSequence()
        .flatMap {
            sequenceOf(
                it.effectUuids,
                it.success?.effectUuids,
                it.criticalSuccess?.effectUuids,
                it.failure?.effectUuids,
                it.criticalFailure?.effectUuids,
            ).filterNotNull()
                .flatMap(Array<ActivityEffect>::asSequence)
                .filter(predicate)
        }
        .toList()
