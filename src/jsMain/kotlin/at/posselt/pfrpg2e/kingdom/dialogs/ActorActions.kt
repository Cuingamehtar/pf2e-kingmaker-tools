package at.posselt.pfrpg2e.kingdom.dialogs

import at.posselt.pfrpg2e.app.HandlebarsRenderContext
import at.posselt.pfrpg2e.app.confirm
import at.posselt.pfrpg2e.app.forms.SimpleApp
import at.posselt.pfrpg2e.app.jsonFilePicker
import at.posselt.pfrpg2e.camping.CampingData
import at.posselt.pfrpg2e.camping.clearCamping
import at.posselt.pfrpg2e.camping.getCamping
import at.posselt.pfrpg2e.camping.setCamping
import at.posselt.pfrpg2e.kingdom.KingdomData
import at.posselt.pfrpg2e.kingdom.clearKingdom
import at.posselt.pfrpg2e.kingdom.getKingdom
import at.posselt.pfrpg2e.kingdom.setKingdom
import at.posselt.pfrpg2e.utils.buildPromise
import at.posselt.pfrpg2e.utils.downloadJson
import com.foundryvtt.core.applications.api.HandlebarsRenderOptions
import com.foundryvtt.pf2e.actor.PF2EParty
import js.objects.JsPlainObject
import kotlinx.coroutines.await
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.js.Promise

@JsPlainObject
external interface ActorActionsContext : HandlebarsRenderContext {
    val hasCamping: Boolean
    val hasKingdom: Boolean
}

class ActorActions(
    private val actor: PF2EParty,
) : SimpleApp<ActorActionsContext>(
    title = actor.name,
    template = "applications/settings/actor-actions.hbs",
    id = "kmActorActions",
) {

    override fun _onClickAction(event: PointerEvent, target: HTMLElement) {
        when (target.dataset["action"]) {
            "export-kingdom" -> actor.getKingdom()?.let {
                downloadJson(it, "Kingdom-${actor.uuid}.json")
                close()
            }

            "export-camping" -> actor.getCamping()?.let {
                downloadJson(it, "Camping-${actor.uuid}.json")
                close()
            }

            "import-kingdom" -> buildPromise {
                val json = jsonFilePicker(title = "Upload Kingdom JSON File", "Kingdom")
                actor.setKingdom(JSON.parse<KingdomData>(json))
                close()
            }

            "import-camping" -> buildPromise {
                val json = jsonFilePicker(title = "Upload Camping JSON File", "Camping")
                actor.setCamping(JSON.parse<CampingData>(json))
                close()
            }

            "reset-kingdom" -> buildPromise {
                if (confirm("Do you really want to erase all kingdom data from actor ${actor.name}?")) {
                    actor.clearKingdom()
                    close()
                }
            }

            "reset-camping" -> buildPromise {
                if (confirm("Do you really want to erase all camping data from actor ${actor.name}?")) {
                    actor.clearCamping()
                    close()
                }
            }
        }
    }

    override fun _preparePartContext(
        partId: String,
        context: HandlebarsRenderContext,
        options: HandlebarsRenderOptions
    ): Promise<ActorActionsContext> = buildPromise {
        val parent = super._preparePartContext(partId, context, options).await()
        ActorActionsContext(
            partId = parent.partId,
            hasKingdom = actor.getKingdom() != null,
            hasCamping = actor.getCamping() != null,
        )
    }
}