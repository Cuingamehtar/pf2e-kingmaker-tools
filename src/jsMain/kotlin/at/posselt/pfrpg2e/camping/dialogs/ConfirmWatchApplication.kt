package at.posselt.pfrpg2e.camping.dialogs

import at.posselt.pfrpg2e.app.FormApp
import at.posselt.pfrpg2e.app.HandlebarsRenderContext
import at.posselt.pfrpg2e.app.forms.CheckboxInput
import at.posselt.pfrpg2e.app.forms.FormElementContext
import at.posselt.pfrpg2e.app.forms.formContext
import at.posselt.pfrpg2e.camping.CampingData
import at.posselt.pfrpg2e.camping.getAllRecipes
import at.posselt.pfrpg2e.camping.getCampingActorsByUuid
import at.posselt.pfrpg2e.resting.getTotalRestDuration
import at.posselt.pfrpg2e.utils.buildPromise
import com.foundryvtt.core.AnyObject
import com.foundryvtt.core.abstract.DataModel
import com.foundryvtt.core.applications.api.HandlebarsRenderOptions
import com.foundryvtt.core.data.dsl.buildSchema
import com.foundryvtt.pf2e.actor.PF2EActor
import js.core.Void
import kotlinx.coroutines.await
import kotlinx.js.JsPlainObject
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.js.Promise

@JsPlainObject
external interface ConfirmWatchContext: HandlebarsRenderContext {
    val formRows: Array<FormElementContext>
    val isFormValid: Boolean
    val saveLabel: String
}

@JsExport
class ConfirmWatchDataModel(val value: AnyObject) : DataModel(value) {
    companion object {
        @Suppress("unused")
        @JsStatic
        fun defineSchema() = buildSchema {
            boolean("enableWatch")
        }
    }
}

@JsPlainObject
external interface ConfirmWatchData {
    val enableWatch: Boolean
}

@JsExport
class ConfirmWatchApplication(
    private val camping: CampingData,
    private val afterSubmit: (Boolean) -> Unit,
) : FormApp<ConfirmWatchContext, ConfirmWatchData>(
    title = "Begin Rest",
    template = "components/forms/application-form.hbs",
    debug = true,
    dataModel = ConfirmWatchDataModel::class.js,
    width = 400,
    id = "kmConfirmWatch"
) {
    private var enableWatch: Boolean = true

    override fun _onClickAction(event: PointerEvent, target: HTMLElement) {
        when (target.dataset["action"]) {
            "save" -> buildPromise {
                close()
                afterSubmit(enableWatch)
            }
        }
    }

    private suspend fun calculateWatch(): String {
        val actorsByUuid = getCampingActorsByUuid(camping.actorUuids).associateBy(PF2EActor::uuid)
        val fullRestDuration = getTotalRestDuration(
            watchers = actorsByUuid.values.filter { !camping.actorUuidsNotKeepingWatch.contains(it.uuid) },
            recipes = camping.getAllRecipes().toList(),
            gunsToClean = camping.gunsToClean,
            increaseActorsKeepingWatch = camping.increaseWatchActorNumber,
            remainingSeconds = camping.watchSecondsRemaining,
            skipWatch = !enableWatch,
        )
        return fullRestDuration.total.label
    }

    override fun _preparePartContext(
        partId: String,
        context: HandlebarsRenderContext,
        options: HandlebarsRenderOptions
    ): Promise<ConfirmWatchContext> = buildPromise {
        val parent = super._preparePartContext(partId, context, options).await()
        val calculateWatch = calculateWatch()
        ConfirmWatchContext(
            partId = parent.partId,
            isFormValid = isFormValid,
            saveLabel = "Rest: $calculateWatch",
            formRows = formContext(
                CheckboxInput(
                    label = "Enable Watch",
                    name = "enableWatch",
                    help = "If enabled, rolls a random encounter check during watch and includes watch in rest duration",
                    stacked = false,
                    value = enableWatch,
                ),
            )
        )
    }

    override fun onParsedSubmit(value: ConfirmWatchData): Promise<Void> = buildPromise {
        enableWatch = value.enableWatch
        undefined
    }

}