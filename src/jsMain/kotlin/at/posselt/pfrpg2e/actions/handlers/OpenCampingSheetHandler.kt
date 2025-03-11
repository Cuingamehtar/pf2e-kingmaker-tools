package at.posselt.pfrpg2e.actions.handlers

import at.posselt.pfrpg2e.actions.ActionDispatcher
import at.posselt.pfrpg2e.actions.ActionMessage
import at.posselt.pfrpg2e.camping.CampingSheet
import at.posselt.pfrpg2e.utils.fromUuidTypeSafe
import at.posselt.pfrpg2e.utils.launch
import com.foundryvtt.core.Game
import com.foundryvtt.pf2e.actor.PF2ENpc
import kotlinx.js.JsPlainObject

@JsPlainObject
external interface OpenCampingSheetAction {
    val actorUuid: String
}

class OpenCampingSheetHandler(
    private val game: Game,
) : ActionHandler(
    action = "openCampingSheet",
    mode = ExecutionMode.OTHERS,
) {
    override suspend fun execute(action: ActionMessage, dispatcher: ActionDispatcher) {
        val data = action.data.unsafeCast<OpenCampingSheetAction>()
        fromUuidTypeSafe<PF2ENpc>(data.actorUuid)?.let { campingActor ->
            CampingSheet(game, campingActor, dispatcher).launch()
        }
    }
}