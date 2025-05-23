package at.posselt.pfrpg2e.kingdom.dialogs

import at.posselt.pfrpg2e.app.awaitablePrompt
import at.posselt.pfrpg2e.app.forms.FormElementContext
import at.posselt.pfrpg2e.app.forms.Select
import at.posselt.pfrpg2e.app.forms.formContext
import at.posselt.pfrpg2e.data.kingdom.leaders.Leader
import at.posselt.pfrpg2e.utils.t
import js.objects.JsPlainObject

@JsPlainObject
external interface PickLeaderContext {
    val formRows: Array<FormElementContext>
}

@JsPlainObject
external interface PickLeaderData {
    val leader: String
}

suspend fun pickLeader(): Leader {
    return awaitablePrompt<PickLeaderData, Leader>(
        title = t("kingdom.pickLeaderToSupport"),
        templateContext = PickLeaderContext(
            formRows = formContext(
                Select.fromEnum<Leader>(
                    name = "leader",
                    value = Leader.RULER,
                )
            )
        ),
        templatePath = "components/forms/form.hbs",
    ) { data, _ -> Leader.fromString(data.leader) ?: Leader.RULER }
}