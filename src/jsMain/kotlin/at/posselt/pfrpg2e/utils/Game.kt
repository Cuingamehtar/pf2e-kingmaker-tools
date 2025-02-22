package at.posselt.pfrpg2e.utils

import at.posselt.pfrpg2e.kingdom.KingdomData
import at.posselt.pfrpg2e.kingdom.LeaderKingdomSkills
import at.posselt.pfrpg2e.kingdom.LeaderSkills
import com.foundryvtt.core.Actor
import com.foundryvtt.core.Game
import com.foundryvtt.pf2e.actor.PF2EActor
import kotlinx.js.JsPlainObject
import kotlin.js.Promise

@JsPlainObject
external interface ToolsMacros {
    val toggleWeatherMacro: () -> Unit
    val toggleShelteredMacro: () -> Unit
    val setCurrentWeatherMacro: () -> Unit
    val sceneWeatherSettingsMacro: () -> Unit
    val kingdomEventsMacro: () -> Unit
    val rollKingmakerWeatherMacro: () -> Unit
    val awardXpMacro: () -> Unit
    val resetHeroPointsMacro: () -> Unit
    val awardHeroPointsMacro: () -> Unit
    val rollExplorationSkillCheck: (String, String) -> Unit
    val rollSkillDialog: () -> Unit
    val setSceneCombatPlaylistDialogMacro: (PF2EActor?) -> Unit
    val toTimeOfDayMacro: () -> Unit
    val toggleCombatTracksMacro: () -> Unit
    val realmTileDialogMacro: () -> Unit
    val editStructureMacro: (Actor?) -> Unit
    val openCampingSheet: () -> Unit
    val createFoodMacro: () -> Unit
    val subsistMacro: (Actor?) -> Unit
//    val structureTokenMappingMacro: () -> Unit
//    val viewKingdomMacro: () -> Unit
}

@JsPlainObject
external interface KtMigration {
    val configureLeaderKingdomSkills: (skills: LeaderKingdomSkills, onSave: (skills: LeaderKingdomSkills) -> Unit) -> Unit
    val configureLeaderSkills: (skills: LeaderSkills, onSave: (skills: LeaderSkills) -> Unit) -> Unit
}

@JsPlainObject
external interface Pfrpg2eKingdomCampingWeather {
    val macros: ToolsMacros
    val migration: KtMigration
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
inline var Game.pf2eKingmakerTools: Pfrpg2eKingdomCampingWeather
    get() = asDynamic().pf2eKingmakerTools as Pfrpg2eKingdomCampingWeather
    set(value) {
        asDynamic().pf2eKingmakerTools = value
    }
