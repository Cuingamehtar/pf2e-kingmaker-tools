package at.posselt.pfrpg2e.kingdom.sheet.contexts

import at.posselt.pfrpg2e.app.forms.CheckboxInput
import at.posselt.pfrpg2e.app.forms.FormElementContext
import at.posselt.pfrpg2e.app.forms.HiddenInput
import at.posselt.pfrpg2e.app.forms.OverrideType
import at.posselt.pfrpg2e.kingdom.RawMilestone
import at.posselt.pfrpg2e.kingdom.data.MilestoneChoice
import kotlinx.js.JsPlainObject

@JsPlainObject
external interface MilestoneContext {
    val name: String
    val xp: Int
    val hidden: Boolean
    val completed: FormElementContext
    val enabled: FormElementContext
    val id: FormElementContext
}

fun Array<MilestoneChoice>.toContext(
    milestones: Array<RawMilestone>,
    isGm: Boolean,
): Array<MilestoneContext> {
    val choicesById = associateBy { it.id }
    return milestones.mapIndexed { index, milestone ->
        val id = milestone.id
        MilestoneContext(
            id = HiddenInput(
                name = "milestones.$index.id",
                value = id,
                label = "Id",
            ).toContext(),
            name = milestone.name,
            xp = milestone.xp,
            completed = CheckboxInput(
                name = "milestones.$index.completed",
                value = choicesById[id]?.completed == true,
                label = milestone.name,
            ).toContext(),
            enabled = HiddenInput(
                name = "milestones.$index.enabled",
                value = (choicesById[id]?.enabled == true).toString(),
                label = "Enabled",
                overrideType = OverrideType.BOOLEAN,
            ).toContext(),
            hidden = choicesById[id]?.enabled != true || (milestone.isCultEvent && isGm),
        )
    }.toTypedArray()
}

