package at.posselt.pfrpg2e.settings

import at.posselt.pfrpg2e.Config
import at.posselt.pfrpg2e.data.checks.RollMode
import at.posselt.pfrpg2e.data.kingdom.KingdomSizeType
import at.posselt.pfrpg2e.deCamelCase
import at.posselt.pfrpg2e.fromCamelCase
import at.posselt.pfrpg2e.toCamelCase
import at.posselt.pfrpg2e.toLabel
import at.posselt.pfrpg2e.utils.newInstance
import at.posselt.pfrpg2e.utils.toMutableRecord
import com.foundryvtt.core.*
import com.foundryvtt.core.abstract.DataModel
import com.foundryvtt.core.applications.api.ApplicationV2
import js.core.JsNumber
import js.objects.ReadonlyRecord
import kotlinx.coroutines.await

enum class SettingsScope {
    CLIENT,
    WORLD;

    companion object {
        fun fromString(value: String) = fromCamelCase<KingdomSizeType>(value)
    }

    val value: String
        get() = toCamelCase()

    val label: String
        get() = toLabel()
}

inline fun <reified T : DataModel> Settings.registerDataModel(
    key: String,
    name: String,
    hint: String? = undefined,
    requiresReload: Boolean = false,
) {
    register<AnyObject>(
        Config.moduleId,
        key,
        SettingsData<AnyObject>(
            name = name,
            hint = hint,
            config = false,
            requiresReload = requiresReload,
            default = T::class.js.newInstance(emptyArray()).toObject(),
            type = T::class.js,
            scope = "world"
        )
    )
}

fun Settings.registerInt(
    key: String,
    name: String,
    hint: String? = undefined,
    default: Int = 0,
    hidden: Boolean = false,
    requiresReload: Boolean = false,
    choices: ReadonlyRecord<String, Int>? = undefined,
) {
    register<Int>(
        Config.moduleId,
        key,
        SettingsData<Int>(
            name = name,
            hint = hint,
            config = !hidden,
            default = default,
            requiresReload = requiresReload,
            type = JsNumber::class.js,
            scope = "world",
            choices = choices,
        )
    )
}

inline fun <reified T : Any> Settings.registerScalar(
    key: String,
    name: String,
    hint: String? = undefined,
    default: T? = undefined,
    hidden: Boolean = false,
    requiresReload: Boolean = false,
    choices: ReadonlyRecord<String, T>? = undefined,
    scope: SettingsScope = SettingsScope.WORLD,
) {
    register<T>(
        Config.moduleId,
        key,
        SettingsData<T>(
            name = name,
            hint = hint,
            config = !hidden,
            default = default,
            requiresReload = requiresReload,
            type = T::class.js,
            scope = scope.value,
            choices = choices,
        )
    )
}

fun Settings.createMenu(
    key: String,
    name: String,
    label: String,
    hint: String? = undefined,
    icon: String? = undefined,
    restricted: Boolean = false,
    app: JsClass<out ApplicationV2>
) {
    registerMenu<ApplicationV2>(
        Config.moduleId,
        key,
        SettingsMenuData<ApplicationV2>(
            name = name,
            label = label,
            hint = hint,
            icon = icon,
            type = app,
            restricted = restricted,
        )
    )
}

fun Settings.getInt(key: String): Int =
    get(Config.moduleId, key)

suspend fun Settings.setInt(key: String, value: Int) {
    set(Config.moduleId, key, value).await()
}

fun Settings.getString(key: String): String =
    get(Config.moduleId, key)

suspend fun Settings.setString(key: String, value: String) {
    set(Config.moduleId, key, value).await()
}

fun Settings.getNullableString(key: String): String? =
    get(Config.moduleId, key)

suspend fun Settings.setNullableString(key: String, value: String?) {
    set(Config.moduleId, key, value).await()
}


fun Settings.getBoolean(key: String): Boolean =
    get(Config.moduleId, key)

suspend fun Settings.setBoolean(key: String, value: Boolean) {
    set(Config.moduleId, key, value).await()
}

fun <T : Any> Settings.getObject(key: String): T =
    get(Config.moduleId, key)

suspend fun Settings.setObject(key: String, value: Any) {
    set(Config.moduleId, key, value).await()
}

val Settings.pfrpg2eKingdomCampingWeather: Pfrpg2eKingdomCampingWeatherSettings
    get() = Pfrpg2eKingdomCampingWeatherSettings

@Suppress("unused", "ClassName")
object Pfrpg2eKingdomCampingWeatherSettings {
    suspend fun setKingdomActiveLeader(value: String?) =
        game.settings.setNullableString("kingdomActiveLeader", value)

    fun getKingdomActiveLeader(): String? =
        game.settings.getNullableString("kingdomActiveLeader")

    suspend fun setEnableTokenMapping(value: Boolean) =
        game.settings.setBoolean("enableTokenMapping", value)

    fun getEnableTokenMapping(): Boolean =
        game.settings.getBoolean("enableTokenMapping")

    suspend fun setLatestMigrationBackup(value: String) =
        game.settings.setString("latestMigrationBackup", value)

    fun getLatestMigrationBackup(): String =
        game.settings.getString("latestMigrationBackup")

    suspend fun setSchemaVersion(value: Int) =
        game.settings.setInt("schemaVersion", value)

    fun getSchemaVersion(): Int =
        game.settings.getInt("schemaVersion")

    suspend fun setWeatherHazardRange(value: Int) =
        game.settings.setInt("weatherHazardRange", value)

    fun getWeatherHazardRange(): Int =
        game.settings.getInt("weatherHazardRange")

    suspend fun setWeatherRollMode(value: RollMode) =
        game.settings.setString("weatherRollMode", value.toCamelCase())

    fun getWeatherRollMode(): RollMode =
        fromCamelCase<RollMode>(game.settings.getString("weatherRollMode"))
            ?: throw IllegalStateException("Null value set for setting 'weatherRollMode'")

    suspend fun setKingdomEventRollMode(value: RollMode) =
        game.settings.setString("kingdomEventRollMode", value.toCamelCase())

    fun getKingdomEventRollMode(): RollMode =
        fromCamelCase<RollMode>(game.settings.getString("kingdomEventRollMode"))
            ?: throw IllegalStateException("Null value set for setting 'kingdomEventRollMode'")

    suspend fun setEnableWeatherSoundFx(value: Boolean) =
        game.settings.setBoolean("enableWeatherSoundFx", value)

    fun getEnableWeatherSoundFx(): Boolean =
        game.settings.getBoolean("enableWeatherSoundFx")

    suspend fun setEnableCombatTracks(value: Boolean) =
        game.settings.setBoolean("enableCombatTracks", value)

    fun getEnableCombatTracks(): Boolean =
        game.settings.getBoolean("enableCombatTracks")

    suspend fun setDisableFirstRunMessage(value: Boolean) =
        game.settings.setBoolean("disableFirstRunMessage", value)

    fun getDisableFirstRunMessage(): Boolean =
        game.settings.getBoolean("disableFirstRunMessage")

    suspend fun setEnableSheltered(value: Boolean) =
        game.settings.setBoolean("enableSheltered", value)

    fun getEnableSheltered(): Boolean =
        game.settings.getBoolean("enableSheltered")

    suspend fun setAutoRollWeather(value: Boolean) =
        game.settings.setBoolean("autoRollWeather", value)

    fun getAutoRollWeather(): Boolean =
        game.settings.getBoolean("autoRollWeather")

    suspend fun setEnableWeather(value: Boolean) =
        game.settings.setBoolean("enableWeather", value)

    fun getEnableWeather(): Boolean =
        game.settings.getBoolean("enableWeather")

    fun getHideBuiltinKingdomSheet(): Boolean =
        game.settings.getBoolean("hideBuiltinKingdomSheet")

    suspend fun setHideBuiltinKingdomSheet(value: Boolean) =
        game.settings.setBoolean("hideBuiltinKingdomSheet", value)

    suspend fun setClimateSettings(settings: ClimateSettings) =
        game.settings.setObject("climate", settings)

    fun getClimateSettings(): ClimateSettings =
        game.settings.getObject("climate")

    suspend fun setCurrentWeatherFx(value: String) =
        game.settings.setString("currentWeatherFx", value)

    fun getCurrentWeatherFx(): String =
        game.settings.getString("currentWeatherFx")

    private object nonUserVisibleSettings {
        val booleans = mapOf(
            "enableSheltered" to false,
            "disableFirstRunMessage" to false,
        )
        val strings = mapOf(
            "currentWeatherFx" to "none",
            "latestMigrationBackup" to "{}"
        )
    }


    fun register() {
        registerSimple(game.settings, nonUserVisibleSettings.strings, hidden = true)
        registerSimple(game.settings, nonUserVisibleSettings.booleans, hidden = true)
        game.settings.registerScalar(
            key = "hideBuiltinKingdomSheet",
            name = "Hide PF2 System Kingdom Sheet Icon",
            hint = "If enabled, hides the built in kingdom sheet icon on the party actor",
            default = false,
            requiresReload = true,
        )
        game.settings.registerScalar<String>(
            key = "kingdomActiveLeader",
            name = "Actively Filtered Kingdom Leader",
            default = null,
            hidden = true,
            scope = SettingsScope.CLIENT,
        )
        game.settings.registerInt(
            key = "schemaVersion",
            name = "Schema Version",
            hidden = true,
        )
        game.settings.registerDataModel<ClimateConfigurationDataModel>(
            key = "climate",
            name = "Climate Settings",
        )
        game.settings.createMenu(
            key = "climateMenu",
            label = "Customize Climate",
            name = "Climate",
            app = ClimateConfiguration::class.js,
        )
        game.settings.registerScalar(
            name = "Enable Weather",
            key = "enableWeather",
            default = true,
        )
        game.settings.registerScalar<Boolean>(
            key = "enableWeatherSoundFx",
            name = "Enable Ambient Weather Sounds",
            hint = "If enabled, will play rain and snow tracks from the official module. You can override this behavior by creating playlists named like \"weather.blizzard\" or \"weather.rain\".",
            default = true,
        )
        game.settings.registerScalar<Boolean>(
            key = "autoRollWeather",
            name = "Auto Roll Weather",
            hint = "When a new day begins (00:00), automatically roll weather",
            default = true,
        )
        game.settings.registerScalar<String>(
            key = "weatherRollMode",
            name = "Weather Roll Mode",
            choices = RollMode.entries.asSequence()
                .map { it.toCamelCase() to it.label }
                .toMutableRecord(),
            default = "gmroll"
        )
        game.settings.registerInt(
            key = "weatherHazardRange",
            name = "Weather Hazard Range",
            default = 4,
            hint = "Roll weather events up and equal to Party Level plus this value"
        )
        game.settings.registerScalar<String>(
            key = "kingdomEventRollMode",
            name = "Kingdom Event Roll Mode",
            choices = RollMode.entries.asSequence()
                .map { it.toCamelCase() to it.label }
                .toMutableRecord(),
            default = "gmroll",
            hidden = true,
        )
        game.settings.registerScalar<Boolean>(
            key = "enableCombatTracks",
            name = "Enable Combat Tracks",
            hint = "If enabled, starts a combat track depending on the current region, actor or scene.",
            default = true,
        )
        game.settings.registerScalar<Boolean>(
            key = "enableTokenMapping",
            name = "Enable Token Mapping",
            hint = "If enabled, automatically maps token images to your bestiary browser based on the documented file structure in the manual if the official module is not enabled",
            default = true,
            requiresReload = true,
        )
    }
}


private inline fun <reified T : Any> registerSimple(
    settings: Settings,
    values: Map<String, T>,
    hidden: Boolean,
) {
    values.forEach { (key, value) ->
        settings.registerScalar<T>(
            key = key,
            default = value,
            name = key.deCamelCase(),
            hidden = hidden,
        )
    }
}

