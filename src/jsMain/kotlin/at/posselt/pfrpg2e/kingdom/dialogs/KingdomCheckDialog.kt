package at.posselt.pfrpg2e.kingdom.dialogs

import at.posselt.pfrpg2e.app.FormApp
import at.posselt.pfrpg2e.app.HandlebarsRenderContext
import at.posselt.pfrpg2e.app.forms.CheckboxInput
import at.posselt.pfrpg2e.app.forms.FormElementContext
import at.posselt.pfrpg2e.app.forms.HiddenInput
import at.posselt.pfrpg2e.app.forms.OverrideType
import at.posselt.pfrpg2e.app.forms.Select
import at.posselt.pfrpg2e.app.forms.SelectOption
import at.posselt.pfrpg2e.data.checks.RollMode
import at.posselt.pfrpg2e.data.kingdom.KingdomPhase
import at.posselt.pfrpg2e.data.kingdom.KingdomSkill
import at.posselt.pfrpg2e.data.kingdom.KingdomSkillRanks
import at.posselt.pfrpg2e.data.kingdom.calculateControlDC
import at.posselt.pfrpg2e.data.kingdom.leaders.Leader
import at.posselt.pfrpg2e.fromCamelCase
import at.posselt.pfrpg2e.kingdom.KingdomActivity
import at.posselt.pfrpg2e.kingdom.armies.getTargetedArmies
import at.posselt.pfrpg2e.kingdom.armies.miredValue
import at.posselt.pfrpg2e.kingdom.armies.wearyValue
import at.posselt.pfrpg2e.kingdom.checkModifiers
import at.posselt.pfrpg2e.kingdom.createExpressionContext
import at.posselt.pfrpg2e.kingdom.getActivity
import at.posselt.pfrpg2e.kingdom.getAllSettlements
import at.posselt.pfrpg2e.kingdom.getKingdom
import at.posselt.pfrpg2e.kingdom.hasAssurance
import at.posselt.pfrpg2e.kingdom.modifiers.Modifier
import at.posselt.pfrpg2e.kingdom.modifiers.evaluation.evaluateGlobalBonuses
import at.posselt.pfrpg2e.kingdom.modifiers.evaluation.evaluateModifiers
import at.posselt.pfrpg2e.kingdom.modifiers.evaluation.includeCapital
import at.posselt.pfrpg2e.kingdom.modifiers.penalties.ArmyConditionInfo
import at.posselt.pfrpg2e.kingdom.parseSkillRanks
import at.posselt.pfrpg2e.kingdom.resolveDc
import at.posselt.pfrpg2e.kingdom.structures.RawSettlement
import at.posselt.pfrpg2e.kingdom.vacancies
import at.posselt.pfrpg2e.toLabel
import at.posselt.pfrpg2e.utils.asSequence
import at.posselt.pfrpg2e.utils.buildPromise
import at.posselt.pfrpg2e.utils.formatAsModifier
import at.posselt.pfrpg2e.utils.launch
import at.posselt.pfrpg2e.utils.toRecord
import com.foundryvtt.core.AnyObject
import com.foundryvtt.core.Game
import com.foundryvtt.core.abstract.DataModel
import com.foundryvtt.core.applications.api.HandlebarsRenderOptions
import com.foundryvtt.core.data.dsl.buildSchema
import com.foundryvtt.pf2e.actor.PF2ENpc
import js.core.Void
import js.objects.ReadonlyRecord
import kotlinx.coroutines.await
import kotlinx.js.JsPlainObject
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.js.Promise

@JsPlainObject
private external interface ModifierContext {
    val label: String
    val type: String
    val modifier: String
    val enabled: FormElementContext
    val id: FormElementContext
    val hidden: Boolean
}

@JsPlainObject
private external interface CheckContext : HandlebarsRenderContext {
    val isFormValid: Boolean
    val settlementInput: FormElementContext
    val leaderInput: FormElementContext
    val rollModeInput: FormElementContext
    val phaseInput: FormElementContext
    val skillInput: FormElementContext
    val dc: FormElementContext
    val assurance: FormElementContext
    val modifiers: Array<ModifierContext>
    val hasAssurance: Boolean
    val checkModifier: Int
}

@JsPlainObject
private external interface CheckData {
    var leader: String
    var rollMode: String
    var phase: String
    var skill: String
    var dc: Int
    var assurance: Boolean
    var modifiers: ReadonlyRecord<String, Boolean>
}

@JsExport
class CheckModel(val value: AnyObject) : DataModel(value) {
    companion object {
        @Suppress("unused")
        @JsStatic
        fun defineSchema() = buildSchema {
            int("dc")
            string("settlement")
            enum<KingdomSkill>("skill")
            enum<Leader>("leader")
            enum<RollMode>("rollMode")
            enum<KingdomPhase>("phase")
            boolean("assurance")
            record("modifiers")
        }
    }
}

private fun createSettlementOptions(
    game: Game,
    settlements: Array<RawSettlement>
): List<SelectOption> {
    return settlements.mapNotNull { settlement ->
        game.scenes.get(settlement.sceneId)?.name?.let {
            SelectOption(settlement.sceneId, it)
        }
    }
}

private fun getValidActivitySkills(
    ranks: KingdomSkillRanks,
    activity: KingdomActivity,
    ignoreSkillRequirements: Boolean,
): Set<KingdomSkill> =
    activity.skills.asSequence()
        .mapNotNull { (name, rank) ->
            KingdomSkill.fromString(name)?.let {
                it to rank
            }
        }
        .filter {
            if (ignoreSkillRequirements) {
                true
            } else {
                ranks.resolve(it.first) >= it.second
            }
        }
        .map { it.first }
        .toSet()

private class KingdomCheckDialog(
    title: String,
    private val game: Game,
    private val kingdomActor: PF2ENpc,
    private val activity: KingdomActivity?,
    private val skill: KingdomSkill?,
    private val baseModifiers: List<Modifier>,
    private val validSkills: Set<KingdomSkill>,
) : FormApp<CheckContext, CheckData>(
    title = title,
    template = "applications/kingdom/check.hbs",
    debug = true,
    dataModel = CheckModel::class.js,
    id = "kmCheck",
) {
    var data: CheckData
    val modifiersById = baseModifiers.associateBy { it.id }

    init {
        val kingdom = kingdomActor.getKingdom()!!
        val vacancies = kingdom.vacancies()
        val kingdomLevel = kingdom.level
        val selectedSkill = if (skill != null) {
            skill
        } else if (activity != null) {
            validSkills.firstOrNull() ?: throw IllegalStateException("Activity without valid skills")
        } else {
            KingdomSkill.AGRICULTURE
        }
        val dc = if (activity == null) {
            calculateControlDC(
                kingdomLevel = kingdomLevel,
                kingdomSize = kingdom.size,
                rulerVacant = vacancies.ruler,
            )
        } else {
            activity.resolveDc(
                kingdomLevel = kingdomLevel,
                kingdomSize = kingdom.size,
                rulerVacant = vacancies.ruler,
                enemyArmyScoutingDcs = game.getTargetedArmies().map { it.system.scouting }
            )
        }
        val phase = if (activity == null) {
            KingdomPhase.EVENT
        } else {
            KingdomPhase.fromString(activity.phase) ?: KingdomPhase.EVENT
        }
        data = CheckData(
            modifiers = baseModifiers.map { it.id to it.enabled }.toRecord(),
            leader = Leader.RULER.value,
            rollMode = RollMode.PUBLICROLL.value,
            skill = selectedSkill.value,
            phase = phase.value,
            assurance = false,
            dc = dc ?: throw IllegalStateException("Check window opened for activity without check"),
        )

    }

    override fun _onClickAction(event: PointerEvent, target: HTMLElement) {
        when (target.dataset["action"]) {
            "roll" -> {
                close()
            }

            "rollWithAssurance" -> {
                close()
            }
        }
    }

    override fun _preparePartContext(
        partId: String,
        context: HandlebarsRenderContext,
        options: HandlebarsRenderOptions
    ): Promise<CheckContext> = buildPromise {
        val parent = super._preparePartContext(partId, context, options).await()
        val kingdom = kingdomActor.getKingdom()!!
        val selectedSkill = KingdomSkill.fromString(data.skill)!!
        val leader = Leader.fromString(data.leader)!!
        val phase = fromCamelCase<KingdomPhase>(data.phase)
        val hasAssurance = kingdom.hasAssurance(selectedSkill)
        val rollOptions = baseModifiers.flatMap { it.rollOptions }.toSet()
        val context = kingdom.createExpressionContext(
            phase = phase,
            activity = activity,
            leader = leader,
            usedSkill = selectedSkill,
            rollOptions = rollOptions,
        )
        val evaluatedModifiers = evaluateModifiers(baseModifiers, context)
        val evaluatedModifiersById = evaluatedModifiers.modifiers.associateBy { it.id }
        console.log("evaluated modifiers", evaluatedModifiers)
        CheckContext(
            partId = parent.partId,
            settlementInput = Select(
                name = "settlement",
                label = "Active Settlement",
                options = createSettlementOptions(game, kingdom.settlements),
                value = kingdom.activeSettlement,
            ).toContext(),
            leaderInput = Select.fromEnum(
                name = "leader",
                label = "Leader",
                value = fromCamelCase<RollMode>(data.leader),
            ).toContext(),
            rollModeInput = Select.fromEnum<RollMode>(
                name = "rollMode",
                label = "Roll Mode",
                value = fromCamelCase<RollMode>(data.rollMode),
                labelFunction = { it.label },
                stacked = false,
            ).toContext(),
            phaseInput = Select.fromEnum(
                label = "Phase",
                name = "phase",
                value = phase,
            ).toContext(),
            skillInput = Select(
                label = "Skill",
                name = "skill",
                value = selectedSkill.value,
                options = validSkills.map {
                    val mod = evaluateModifiers(baseModifiers, context.copy(usedSkill = it)).total
                    val label = "${it.label} (${mod.formatAsModifier()})"
                    SelectOption(label, it.value)
                },
            ).toContext(),
            dc = Select.dc(
                name = "dc",
                label = "DC",
                value = data.dc,
            ).toContext(),
            assurance = if (hasAssurance) {
                CheckboxInput(
                    name = "assurance",
                    label = "Assurance",
                    value = data.assurance,
                ).toContext()
            } else {
                HiddenInput(
                    name = "assurance",
                    value = data.assurance.toString(),
                    overrideType = OverrideType.BOOLEAN,
                ).toContext()
            },
            checkModifier = if (data.assurance) {
                evaluatedModifiers.assurance
            } else {
                evaluatedModifiers.total
            },
            hasAssurance = hasAssurance,
            modifiers = baseModifiers
                .sortedBy { it.type }
                .mapIndexed { index, mod -> toModifierContext(mod, evaluatedModifiersById, index) }
                .toTypedArray(),
            isFormValid = true,
        )
    }

    fun toModifierContext(
        modifier: Modifier,
        evaluatedModifiersById: Map<String, Modifier>,
        index: Int,
    ): ModifierContext {
        val hidden = modifier.id !in evaluatedModifiersById
        val evaluatedModifier = evaluatedModifiersById[modifier.id]
        val value = evaluatedModifier?.value ?: 0
        val enabled = evaluatedModifier?.enabled ?: modifier.enabled
        // TODO: figure out enabled
        return ModifierContext(
            label = modifier.name,
            type = modifier.type.label,
            modifier = value.formatAsModifier(),
            hidden = hidden,
            id = HiddenInput(
                name = "modifier.$index.id",
                value = modifier.id,
            ).toContext(),
            enabled = if (hidden) {
                HiddenInput(
                    name = "modifier.$index.enabled",
                    value = enabled.toString(),
                    overrideType = OverrideType.BOOLEAN,
                ).toContext()
            } else {
                CheckboxInput(
                    label = modifier.name,
                    name = "modifier.$index.enabled",
                    value = enabled,
                    hideLabel = true,
                ).toContext()
            }
        )
    }

    override fun onParsedSubmit(value: CheckData): Promise<Void> = buildPromise {
        data = value
        null
    }
}


suspend fun kingdomCheckDialog(
    game: Game,
    kingdomActor: PF2ENpc,
    activityId: String?,
    skill: KingdomSkill?,
) {
    // do as much work as possible before launching check dialog
    val kingdom = kingdomActor.getKingdom() ?: return
    val targetedArmy = game.getTargetedArmies()
        .firstOrNull()
        ?.let {
            ArmyConditionInfo(
                armyName = it.name,
                armyUuid = it.uuid,
                miredValue = it.miredValue() ?: 0,
                wearyValue = it.wearyValue() ?: 0,
            )
        }
    val settlementResult = kingdom.getAllSettlements(game)
    val allSettlements = settlementResult.allSettlements
    val globalBonuses = evaluateGlobalBonuses(allSettlements)
    val currentSettlement = settlementResult.current?.let {
        includeCapital(
            settlement = it,
            capital = settlementResult.capital,
            capitalModifierFallbackEnabled = kingdom.settings.includeCapitalItemModifier
        )
    }

    val activity = activityId?.let { kingdom.getActivity(it) }
    val validSkills = if (activity != null) {
        getValidActivitySkills(
            kingdom.parseSkillRanks(),
            activity,
            kingdom.settings.kingdomIgnoreSkillRequirements,
        )
    } else {
        KingdomSkill.entries.toSet()
    }
    KingdomCheckDialog(
        title = activity?.title ?: skill?.toLabel() ?: "Check",
        game = game,
        kingdomActor = kingdomActor,
        activity = activity,
        skill = skill,
        validSkills = validSkills,
        baseModifiers = kingdom.checkModifiers(
            globalBonuses = globalBonuses,
            currentSettlement = currentSettlement,
            allSettlements = allSettlements,
            targetedArmy = targetedArmy,
        ),
    ).launch()
}