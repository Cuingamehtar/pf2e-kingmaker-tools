package at.posselt.pfrpg2e.actions.handlers

import at.posselt.pfrpg2e.actions.ActionDispatcher
import at.posselt.pfrpg2e.actions.ActionMessage
import at.posselt.pfrpg2e.kingdom.sheet.KingdomSheet
import at.posselt.pfrpg2e.utils.fromUuidTypeSafe
import at.posselt.pfrpg2e.utils.launch
import com.foundryvtt.core.Game
import com.foundryvtt.pf2e.actor.PF2ENpc
import kotlinx.js.JsPlainObject

@JsPlainObject
external interface OpenKingdomSheetAction {
    val actorUuid: String
}

class OpenKingdomSheetHandler(
    private val game: Game,
) : ActionHandler(
    action = "openKingdomSheet",
    mode = ExecutionMode.OTHERS,
) {
    override suspend fun execute(action: ActionMessage, dispatcher: ActionDispatcher) {
        val data = action.data.unsafeCast<OpenKingdomSheetAction>()
        fromUuidTypeSafe<PF2ENpc>(data.actorUuid)?.let {
            KingdomSheet(game, it, dispatcher).launch()
        }
    }
}