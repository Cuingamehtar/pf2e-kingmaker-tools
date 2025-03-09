package at.posselt.pfrpg2e.kingdom.dialogs

import at.posselt.pfrpg2e.app.FormApp
import at.posselt.pfrpg2e.app.HandlebarsRenderContext
import at.posselt.pfrpg2e.app.forms.CheckboxInput
import at.posselt.pfrpg2e.app.forms.FormElementContext
import at.posselt.pfrpg2e.app.forms.HiddenInput
import at.posselt.pfrpg2e.app.forms.NumberInput
import at.posselt.pfrpg2e.app.forms.OverrideType
import at.posselt.pfrpg2e.app.forms.Select
import at.posselt.pfrpg2e.app.forms.SelectOption
import at.posselt.pfrpg2e.app.forms.TextInput
import at.posselt.pfrpg2e.data.checks.DegreeOfSuccess
import at.posselt.pfrpg2e.data.checks.RollMode
import at.posselt.pfrpg2e.data.checks.determineDegreeOfSuccess
import at.posselt.pfrpg2e.data.kingdom.KingdomPhase
import at.posselt.pfrpg2e.data.kingdom.KingdomSkill
import at.posselt.pfrpg2e.data.kingdom.KingdomSkillRank
import at.posselt.pfrpg2e.data.kingdom.KingdomSkillRanks
import at.posselt.pfrpg2e.data.kingdom.calculateControlDC
import at.posselt.pfrpg2e.data.kingdom.leaders.Leader
import at.posselt.pfrpg2e.data.kingdom.structures.Structure
import at.posselt.pfrpg2e.fromCamelCase
import at.posselt.pfrpg2e.kingdom.KingdomActivity
import at.posselt.pfrpg2e.kingdom.KingdomData
import at.posselt.pfrpg2e.kingdom.armies.getTargetedArmies
import at.posselt.pfrpg2e.kingdom.armies.getTargetedArmyConditions
import at.posselt.pfrpg2e.kingdom.checkModifiers
import at.posselt.pfrpg2e.kingdom.createExpressionContext
import at.posselt.pfrpg2e.kingdom.data.ChosenFeat
import at.posselt.pfrpg2e.kingdom.data.getChosenFeats
import at.posselt.pfrpg2e.kingdom.data.getChosenFeatures
import at.posselt.pfrpg2e.kingdom.data.getChosenGovernment
import at.posselt.pfrpg2e.kingdom.getAllActivities
import at.posselt.pfrpg2e.kingdom.getAllSettlements
import at.posselt.pfrpg2e.kingdom.getExplodedFeatures
import at.posselt.pfrpg2e.kingdom.getRealmData
import at.posselt.pfrpg2e.kingdom.hasAssurance
import at.posselt.pfrpg2e.kingdom.increasedSkills
import at.posselt.pfrpg2e.kingdom.modifiers.Modifier
import at.posselt.pfrpg2e.kingdom.modifiers.ModifierType
import at.posselt.pfrpg2e.kingdom.modifiers.evaluation.evaluateGlobalBonuses
import at.posselt.pfrpg2e.kingdom.modifiers.evaluation.evaluateModifiers
import at.posselt.pfrpg2e.kingdom.modifiers.evaluation.filterModifiersAndUpdateContext
import at.posselt.pfrpg2e.kingdom.modifiers.evaluation.includeCapital
import at.posselt.pfrpg2e.kingdom.modifiers.expressions.ExpressionContext
import at.posselt.pfrpg2e.kingdom.modifiers.penalties.ArmyConditionInfo
import at.posselt.pfrpg2e.kingdom.parse
import at.posselt.pfrpg2e.kingdom.parseModifiers
import at.posselt.pfrpg2e.kingdom.parseSkillRanks
import at.posselt.pfrpg2e.kingdom.resolveDc
import at.posselt.pfrpg2e.kingdom.setKingdom
import at.posselt.pfrpg2e.kingdom.skillRanks
import at.posselt.pfrpg2e.kingdom.structures.RawSettlement
import at.posselt.pfrpg2e.kingdom.vacancies
import at.posselt.pfrpg2e.utils.buildPromise
import at.posselt.pfrpg2e.utils.deserializeB64Json
import at.posselt.pfrpg2e.utils.formatAsModifier
import at.posselt.pfrpg2e.utils.launch
import at.posselt.pfrpg2e.utils.postChatMessage
import at.posselt.pfrpg2e.utils.serializeB64Json
import com.foundryvtt.core.AnyObject
import com.foundryvtt.core.Game
import com.foundryvtt.core.abstract.DataModel
import com.foundryvtt.core.applications.api.HandlebarsRenderOptions
import com.foundryvtt.core.data.dsl.buildSchema
import com.foundryvtt.pf2e.actor.PF2ENpc
import io.github.uuidjs.uuid.v4
import js.core.Void
import kotlinx.coroutines.await
import kotlinx.js.JsPlainObject
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.Array
import kotlin.Boolean
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.arrayOf
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.collections.toSet
import kotlin.collections.toTypedArray
import kotlin.js.Promise
import kotlin.let
import kotlin.math.max
import kotlin.sequences.filter
import kotlin.sequences.flatMap
import kotlin.sequences.map
import kotlin.sequences.mapNotNull
import kotlin.sequences.toSet
import kotlin.text.toInt

@JsPlainObject
private external interface ModifierContext {
    val label: String
    val type: String
    val modifier: String
    val enabledInput: FormElementContext
    val idInput: FormElementContext
    val hidden: Boolean
    val id: String
    val removable: Boolean
    val fortune: Boolean
}

@JsPlainObject
private external interface CheckContext : HandlebarsRenderContext {
    val isFormValid: Boolean
    val leaderInput: FormElementContext
    val rollModeInput: FormElementContext
    val phaseInput: FormElementContext
    val skillInput: FormElementContext
    val dcInput: FormElementContext
    val assuranceInput: FormElementContext
    val modifiers: Array<ModifierContext>
    val hasAssurance: Boolean
    val useAssurance: Boolean
    val checkModifier: Int
    val checkModifierLabel: String
    val supernaturalSolutionInput: FormElementContext
    val newModifierNameInput: FormElementContext
    val newModifierTypeInput: FormElementContext
    val newModifierModifierInput: FormElementContext
    val pills: String
    val upgrades: String
    val isActivity: Boolean
    val assuranceModifier: Int
    val assuranceDegree: String
    val creativeSolutionModifier: Int
    val creativeSolutionPills: String
    val fortune: Boolean
    val downgrades: String
    val consumeModifiers: String
}

@JsPlainObject
private external interface ModifierIdEnabled {
    var id: String
    var enabled: Boolean
}


@JsPlainObject
private external interface CheckData {
    var leader: String
    var rollMode: String
    var phase: String?
    var skill: String
    var dc: Int
    var assurance: Boolean
    var modifiers: Array<ModifierIdEnabled>
    var supernaturalSolution: Boolean
    var newModifierName: String
    var newModifierType: String
    var newModifierModifier: Int
}

@JsExport
class CheckModel(val value: AnyObject) : DataModel(value) {
    companion object {
        @Suppress("unused")
        @JsStatic
        fun defineSchema() = buildSchema {
            int("dc")
            string("settlement")
            boolean("supernaturalSolution") {
                initial = false
            }
            enum<KingdomSkill>("skill")
            enum<Leader>("leader")
            enum<RollMode>("rollMode")
            enum<KingdomPhase>("phase", nullable = true)
            string("newModifierName")
            enum<ModifierType>("newModifierType")
            int("newModifierModifier")
            boolean("assurance")
            array("modifiers") {
                schema {
                    string("id")
                    boolean("enabled")
                }
            }
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

private val expandMagicActivities = setOf(
    "celebrate-holiday",
    "celebrate-holiday-vk",
    "craft-luxuries",
    "create-a-masterpiece",
    "rest-and-relax",
)

private fun getValidActivitySkills(
    ranks: KingdomSkillRanks,
    activityRanks: Set<KingdomSkillRank>,
    ignoreSkillRequirements: Boolean,
    activityId: String?,
    expandMagicUse: Boolean,
    increaseSkills: List<Map<KingdomSkill, Set<KingdomSkill>>>,
): Set<KingdomSkill> {
    val skills = activityRanks.asSequence()
        .filter {
            if (ignoreSkillRequirements) {
                true
            } else {
                ranks.resolve(it.skill) >= it.rank
            }
        }
        .map { it.skill }
        .toSet() + if (expandMagicUse && activityId in expandMagicActivities) {
        setOf(KingdomSkill.MAGIC)
    } else {
        emptySet()
    }
    val groupedSkills = increaseSkills
        .fold(emptyMap<KingdomSkill, Set<KingdomSkill>>()) { prev, curr ->
            prev + curr.mapValues { (k, v) -> v + prev[k].orEmpty() }
        }
    val featSkills = skills.flatMap { groupedSkills[it] ?: emptySet() }.toSet()
    return skills + featSkills
}

private data class CheckDialogParams(
    val title: String,
    val dc: Int,
    val validSkills: Set<KingdomSkill>,
    val phase: KingdomPhase?,
    val structure: Structure?,
    val activity: KingdomActivity?,
    val armyConditions: ArmyConditionInfo?,
)

typealias AfterRollMessage = suspend (degree: DegreeOfSuccess) -> String?

private class KingdomCheckDialog(
    private val kingdomActor: PF2ENpc,
    private val kingdom: KingdomData,
    private var baseModifiers: List<Modifier>,
    private val afterRoll: AfterRollMessage,
    params: CheckDialogParams,
) : FormApp<CheckContext, CheckData>(
    title = params.title,
    template = "applications/kingdom/check.hbs",
    debug = true,
    dataModel = CheckModel::class.js,
    id = "kmCheck",
    width = 600,
) {
    val activity = params.activity
    val validSkills = params.validSkills
    val structure = params.structure
    val removableIds = mutableSetOf<String>()

    var data = CheckData(
        modifiers = baseModifiers.map { ModifierIdEnabled(it.id, it.enabled) }.toTypedArray(),
        leader = Leader.RULER.value,
        rollMode = RollMode.PUBLICROLL.value,
        skill = params.validSkills.first().value,
        phase = params.phase?.value,
        assurance = false,
        dc = params.dc,
        supernaturalSolution = false,
        newModifierName = "",
        newModifierType = "untyped",
        newModifierModifier = 0,
    )

    override fun _onClickAction(event: PointerEvent, target: HTMLElement) {
        when (target.dataset["action"]) {
            "roll" -> {
                buildPromise {
                    val rollTwice = target.dataset["rollTwice"] == "true"
                    val fortune = target.dataset["fortune"] == "true"
                    val modifier = target.dataset["modifier"]?.toInt() ?: 0
                    val creativeSolutionModifier = target.dataset["creativeSolutionModifier"]?.toInt() ?: 0
                    val pills = deserializeB64Json<Array<ModifierPill>>(target.dataset["pills"]!!)
                    val creativeSolutionPills =
                        deserializeB64Json<Array<ModifierPill>>(target.dataset["creativeSolutionPills"]!!)
                    val upgrades = deserializeB64Json<Array<String>>(target.dataset["upgrades"]!!)
                        .mapNotNull { DegreeOfSuccess.fromString(it) }
                        .toSet()
                    val downgrades = deserializeB64Json<Array<String>>(target.dataset["downgrades"]!!)
                        .mapNotNull { DegreeOfSuccess.fromString(it) }
                        .toSet()
                    val consumedModifiers =
                        deserializeB64Json<Array<String>>(target.dataset["consumeModifiers"]!!).toSet()
                    roll(
                        modifier = modifier,
                        pills = pills,
                        upgrades = upgrades,
                        downgrades = downgrades,
                        fortune = fortune,
                        rollTwice = rollTwice,
                        creativeSolutionModifier = creativeSolutionModifier,
                        creativeSolutionPills = creativeSolutionPills,
                        consumedModifiers = consumedModifiers,
                    )
                    close()
                }
            }

            "add-modifier" -> {
                val id = v4()
                data.modifiers = data.modifiers + ModifierIdEnabled(id, true)
                removableIds.add(id)
                baseModifiers = baseModifiers + Modifier(
                    id = id,
                    type = ModifierType.fromString(data.newModifierType) ?: ModifierType.UNTYPED,
                    value = data.newModifierModifier,
                    name = data.newModifierName,
                )
                data.newModifierName = ""
                data.newModifierType = ModifierType.UNTYPED.value
                data.newModifierModifier = 0
                render()
            }

            "delete-modifier" -> {
                val id = target.dataset["id"]
                if (id != null) {
                    baseModifiers = baseModifiers.filter { it.id != id }
                    removableIds.remove(id)
                    data.modifiers = data.modifiers.filter { it.id != id }.toTypedArray()
                    render()
                }
            }
        }
    }

    private suspend fun roll(
        modifier: Int,
        creativeSolutionModifier: Int,
        creativeSolutionPills: Array<ModifierPill>,
        pills: Array<ModifierPill>,
        upgrades: Set<DegreeOfSuccess>,
        fortune: Boolean,
        rollTwice: Boolean,
        downgrades: Set<DegreeOfSuccess>,
        consumedModifiers: Set<String>,
    ) {
        rollCheck(
            afterRoll = afterRoll,
            rollMode = RollMode.fromString(data.rollMode),
            activity = activity,
            skill = KingdomSkill.fromString(data.skill)!!,
            modifier = modifier,
            modifierWithCreativeSolution = creativeSolutionModifier,
            fortune = fortune,
            modifierPills = pills,
            dc = data.dc,
            kingdomActor = kingdomActor,
            upgrades = upgrades,
            rollTwice = rollTwice,
            creativeSolutionPills = creativeSolutionPills,
            downgrades = downgrades,
        )
        if (data.supernaturalSolution && !data.assurance) {
            // TODO launch another check dialog with magic as the only skill and disable supernatural solution input
            postChatMessage("Reduced Supernatural Solutions by 1")
            kingdom.supernaturalSolutions = max(0, kingdom.supernaturalSolutions - 1)
        }

        kingdom.modifiers = kingdom.modifiers.filter { it.id !in consumedModifiers }.toTypedArray()
        kingdomActor.setKingdom(kingdom)
    }

    override fun _preparePartContext(
        partId: String,
        context: HandlebarsRenderContext,
        options: HandlebarsRenderOptions
    ): Promise<CheckContext> = buildPromise {
        val parent = super._preparePartContext(partId, context, options).await()
        val currentlyEnabledModIds = data.modifiers.filter { it.enabled }.map { it.id }.toSet()
        val enabledModifiers = baseModifiers.map { it.copy(enabled = it.id in currentlyEnabledModIds) }
        val phase = data.phase?.let { fromCamelCase<KingdomPhase>(it) }
        val usedSkill = KingdomSkill.fromString(data.skill)!!
        val context = kingdom.createExpressionContext(
            phase = phase,
            activity = activity,
            leader = Leader.fromString(data.leader)!!,
            usedSkill = if (data.supernaturalSolution) KingdomSkill.MAGIC else usedSkill,
            rollOptions = emptySet(),
            structure = structure,
        )
        val filtered = filterModifiersAndUpdateContext(enabledModifiers, context)
        val evaluatedModifiers = evaluateModifiers(filtered)
        val creativeSolutionModifiers = evaluateModifiers(
            filtered.copy(
                context = filtered.context.copy(rollOptions = filtered.context.rollOptions + "creative-solution")
            )
        )
        val chosenFeatures = kingdom.getChosenFeatures(kingdom.getExplodedFeatures())
        val chosenFeats = kingdom.getChosenFeats(chosenFeatures)
        val upgradeDegrees = getUpgrades(chosenFeats, context, evaluatedModifiers.upgradeResults)
        val downgradeDegrees = evaluatedModifiers.downgradeResults
        val selectedSkill = context.usedSkill
        val hasAssurance = kingdom.hasAssurance(chosenFeats, selectedSkill)
        val evaluatedModifiersById = evaluatedModifiers.modifiers.associateBy { it.id }
        val consumeModifierIds = evaluatedModifiers.modifiers
            .filter { it.isConsumedAfterRoll }
            .map { it.id }
            .toTypedArray()
        val pills = if (data.assurance) {
            serializeB64Json(
                arrayOf(
                    ModifierPill(label = "Assurance", value = evaluatedModifiers.total.formatAsModifier())
                )
            )
        } else {
            serializeB64Json(evaluatedModifiers.modifiers.map {
                ModifierPill(label = it.name, value = it.value.formatAsModifier())
            }.toTypedArray())
        }
        val creativeSolutionPills = serializeB64Json(creativeSolutionModifiers.modifiers.map {
            ModifierPill(label = it.name, value = it.value.formatAsModifier())
        }.toTypedArray())
        val checkModifier = if (data.assurance) {
            evaluatedModifiers.assurance
        } else {
            evaluatedModifiers.total
        }
        if (evaluatedModifiers.fortune) {
            data.supernaturalSolution = false
        }
        CheckContext(
            partId = parent.partId,
            leaderInput = Select.fromEnum<Leader>(
                name = "leader",
                label = "Leader",
                value = fromCamelCase<Leader>(data.leader),
            ).toContext(),
            rollModeInput = Select.fromEnum<RollMode>(
                name = "rollMode",
                label = "Roll Mode",
                value = fromCamelCase<RollMode>(data.rollMode),
                labelFunction = { it.label },
                hideLabel = true,
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
                    val cx = filtered.context.copy(usedSkill = it)
                    val mod = evaluateModifiers(filtered.copy(context = cx)).total
                    val label = "${it.label} (${mod.formatAsModifier()})"
                    SelectOption(label, it.value)
                },
            ).toContext(),
            dcInput = Select.dc(
                name = "dc",
                label = "DC",
                value = data.dc,
            ).toContext(),
            supernaturalSolutionInput = CheckboxInput(
                name = "supernaturalSolution",
                label = "Supernatural Solution",
                value = data.supernaturalSolution,
                disabled = kingdom.supernaturalSolutions == 0 || evaluatedModifiers.fortune
            ).toContext(),
            assuranceInput = if (hasAssurance) {
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
            checkModifier = checkModifier,
            hasAssurance = hasAssurance,
            modifiers = enabledModifiers
                .mapIndexed { index, mod -> toModifierContext(mod, evaluatedModifiersById, index) }
                .sortedWith(compareBy<ModifierContext> { !it.hidden }.thenBy { it.type })
                .toTypedArray(),
            pills = pills,
            upgrades = serializeB64Json(upgradeDegrees.map { it.value }.toTypedArray()),
            downgrades = serializeB64Json(downgradeDegrees.map { it.value }.toTypedArray()),
            useAssurance = data.assurance,
            isFormValid = true,
            isActivity = activity != null,
            checkModifierLabel = checkModifier.formatAsModifier(),
            newModifierNameInput = TextInput(
                label = "Name",
                value = data.newModifierName,
                name = "newModifierName",
                required = false,
            ).toContext(),
            newModifierTypeInput = Select.fromEnum<ModifierType>(
                name = "newModifierType",
                label = "Type",
                value = ModifierType.fromString(data.newModifierType) ?: ModifierType.UNTYPED,
            ).toContext(),
            newModifierModifierInput = NumberInput(
                label = "Modifier",
                value = data.newModifierModifier,
                name = "newModifierModifier",
            ).toContext(),
            assuranceModifier = checkModifier,
            assuranceDegree = determineAssuranceDegree(checkModifier, upgradeDegrees, downgradeDegrees),
            creativeSolutionModifier = creativeSolutionModifiers.total,
            creativeSolutionPills = creativeSolutionPills,
            fortune = evaluatedModifiers.fortune || data.supernaturalSolution,
            consumeModifiers = serializeB64Json(consumeModifierIds)
        )
    }

    private fun determineAssuranceDegree(
        modifier: Int,
        upgradeDegrees: Set<DegreeOfSuccess>,
        downgradeDegrees: Set<DegreeOfSuccess>
    ): String {
        val degree = determineDegreeOfSuccess(data.dc, modifier, 10)
        val resultUpgrade = if (degree in upgradeDegrees) degree.upgrade() else degree
        val result = if (degree in downgradeDegrees) resultUpgrade.downgrade() else resultUpgrade
        return result.label
    }

    private fun getUpgrades(
        chosenFeats: List<ChosenFeat>,
        context: ExpressionContext,
        modifierUpgrades: Set<DegreeOfSuccess>
    ): Set<DegreeOfSuccess> =
        chosenFeats.asSequence()
            .flatMap { it.feat.upgradeResults?.toList().orEmpty() }
            .map { it.parse() }
            .filter { it?.applyIf?.all { exp -> exp.evaluate(context) } != false }
            .mapNotNull { it?.upgrade }
            .toSet() + modifierUpgrades

    fun toModifierContext(
        modifier: Modifier,
        evaluatedModifiersById: Map<String, Modifier>,
        index: Int,
    ): ModifierContext {
        val id = modifier.id
        val evaluatedModifier = evaluatedModifiersById[id]
        val value = evaluatedModifier?.value ?: 0
        val hidden = id !in evaluatedModifiersById || data.assurance && modifier.type != ModifierType.PROFICIENCY
        val enabled = evaluatedModifier?.enabled ?: modifier.enabled
        return ModifierContext(
            label = modifier.name,
            type = modifier.type.label,
            modifier = value.formatAsModifier(),
            id = modifier.id,
            removable = modifier.id in removableIds,
            fortune = modifier.fortune,
            hidden = hidden,
            idInput = HiddenInput(
                name = "modifiers.$index.id",
                value = id,
            ).toContext(),
            enabledInput = if (hidden) {
                HiddenInput(
                    name = "modifiers.$index.enabled",
                    value = enabled.toString(),
                    overrideType = OverrideType.BOOLEAN,
                ).toContext()
            } else {
                CheckboxInput(
                    label = "Enable",
                    name = "modifiers.$index.enabled",
                    value = enabled,
                ).toContext()
            }
        )
    }

    override fun onParsedSubmit(value: CheckData): Promise<Void> = buildPromise {
        data = value
        null
    }
}

sealed interface CheckType {
    value class RollSkill(val skill: KingdomSkill) : CheckType
    value class PerformActivity(val activity: KingdomActivity) : CheckType
    value class BuildStructure(val structure: Structure) : CheckType
}

suspend fun kingdomCheckDialog(
    game: Game,
    kingdom: KingdomData,
    kingdomActor: PF2ENpc,
    check: CheckType,
    afterRoll: AfterRollMessage = { "" },
    overrideSkills: Set<KingdomSkillRank>? = null,
    overrideDc: Int? = null,
) {
    // TODO: create chat data for constructing structures
    val params = when (check) {
        is CheckType.PerformActivity -> {
            val activity = check.activity
            val realm = game.getRealmData(kingdom)
            val vacancies = kingdom.vacancies()
            val dc = overrideDc ?: activity.resolveDc(
                kingdomLevel = kingdom.level,
                realm = realm,
                rulerVacant = vacancies.ruler,
                enemyArmyScoutingDcs = game.getTargetedArmies().map { it.system.scouting }
            )
            val chosenFeatures = kingdom.getChosenFeatures(kingdom.getExplodedFeatures())
            val chosenFeats = kingdom.getChosenFeats(chosenFeatures)
            val skills = getValidActivitySkills(
                ranks = kingdom.parseSkillRanks(
                    chosenFeatures,
                    chosenFeats,
                    kingdom.getChosenGovernment()
                ),
                activityRanks = overrideSkills ?: activity.skillRanks(),
                ignoreSkillRequirements = kingdom.settings.kingdomIgnoreSkillRequirements,
                expandMagicUse = kingdom.settings.expandMagicUse,
                activityId = activity.id,
                increaseSkills = chosenFeats.map { it.feat.increasedSkills() }
            )
            CheckDialogParams(
                title = activity.title,
                dc = overrideDc ?: dc ?: 0,
                validSkills = skills,
                phase = KingdomPhase.fromString(activity.phase),
                armyConditions = game.getTargetedArmyConditions(),
                structure = null,
                activity = activity,
            )
        }

        is CheckType.RollSkill -> {
            val realm = game.getRealmData(kingdom)
            val vacancies = kingdom.vacancies()
            val dc = overrideDc ?: calculateControlDC(
                kingdomLevel = kingdom.level,
                realm = realm,
                rulerVacant = vacancies.ruler,
            )
            CheckDialogParams(
                title = check.skill.label, dc = dc, validSkills = setOf(check.skill),
                phase = KingdomPhase.EVENT,
                structure = null,
                activity = null,
                armyConditions = null,
            )
        }

        is CheckType.BuildStructure -> {
            val structure = check.structure
            val activity = kingdom.getAllActivities().find { it.id == "build-structure" }
                ?: throw IllegalArgumentException("No Build Structure Activity present")
            val dc = structure.construction.dc
            val chosenFeatures = kingdom.getChosenFeatures(kingdom.getExplodedFeatures())
            val chosenFeats = kingdom.getChosenFeats(chosenFeatures)
            val skills = getValidActivitySkills(
                ranks = kingdom.parseSkillRanks(
                    chosenFeatures,
                    chosenFeats,
                    kingdom.getChosenGovernment()
                ),
                activityRanks = overrideSkills ?: structure.construction.skills,
                ignoreSkillRequirements = kingdom.settings.kingdomIgnoreSkillRequirements,
                expandMagicUse = kingdom.settings.expandMagicUse,
                activityId = activity.id,
                increaseSkills = chosenFeats.map { it.feat.increasedSkills() }
            )
            CheckDialogParams(
                title = activity.title,
                dc = dc,
                validSkills = skills,
                phase = KingdomPhase.fromString(activity.phase),
                structure = structure,
                activity = activity,
                armyConditions = null,
            )
        }
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
    val baseModifiers = kingdom.checkModifiers(
        globalBonuses = globalBonuses,
        currentSettlement = currentSettlement,
        allSettlements = allSettlements,
        armyConditions = params.armyConditions,
    ) + params.activity?.parseModifiers().orEmpty()
    KingdomCheckDialog(
        params = params,
        afterRoll = afterRoll,
        kingdomActor = kingdomActor,
        kingdom = kingdom,
        baseModifiers = baseModifiers,
    ).launch()
}