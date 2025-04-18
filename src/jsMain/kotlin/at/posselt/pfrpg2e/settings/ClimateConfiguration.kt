package at.posselt.pfrpg2e.settings

import at.posselt.pfrpg2e.app.FormApp
import at.posselt.pfrpg2e.app.HandlebarsRenderContext
import at.posselt.pfrpg2e.app.ValidatedHandlebarsContext
import at.posselt.pfrpg2e.app.forms.Select
import at.posselt.pfrpg2e.camping.dialogs.TableHead
import at.posselt.pfrpg2e.data.regions.Month
import at.posselt.pfrpg2e.data.regions.Season
import at.posselt.pfrpg2e.toCamelCase
import at.posselt.pfrpg2e.utils.buildPromise
import at.posselt.pfrpg2e.utils.t
import com.foundryvtt.core.AnyObject
import com.foundryvtt.core.abstract.DataModel
import com.foundryvtt.core.abstract.DocumentConstructionContext
import com.foundryvtt.core.applications.api.HandlebarsRenderOptions
import com.foundryvtt.core.data.dsl.buildSchema
import com.foundryvtt.core.game
import kotlinx.coroutines.await
import kotlinx.js.JsPlainObject
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.js.Promise

fun getDefaultMonths(): Array<ClimateSetting> = arrayOf(
    ClimateSetting(season = Season.WINTER.toCamelCase(), precipitationDc = 8, coldDc = 16, weatherEventDc = 18),
    ClimateSetting(season = Season.WINTER.toCamelCase(), precipitationDc = 8, coldDc = 18, weatherEventDc = 18),
    ClimateSetting(season = Season.SPRING.toCamelCase(), precipitationDc = 15, weatherEventDc = 18),
    ClimateSetting(season = Season.SPRING.toCamelCase(), precipitationDc = 15, weatherEventDc = 18),
    ClimateSetting(season = Season.SPRING.toCamelCase(), precipitationDc = 15, weatherEventDc = 18),
    ClimateSetting(season = Season.SUMMER.toCamelCase(), precipitationDc = 20, weatherEventDc = 18),
    ClimateSetting(season = Season.SUMMER.toCamelCase(), precipitationDc = 20, weatherEventDc = 18),
    ClimateSetting(season = Season.SUMMER.toCamelCase(), precipitationDc = 20, weatherEventDc = 18),
    ClimateSetting(season = Season.FALL.toCamelCase(), precipitationDc = 15, weatherEventDc = 18),
    ClimateSetting(season = Season.FALL.toCamelCase(), precipitationDc = 15, weatherEventDc = 18),
    ClimateSetting(season = Season.FALL.toCamelCase(), precipitationDc = 15, weatherEventDc = 18),
    ClimateSetting(season = Season.WINTER.toCamelCase(), precipitationDc = 8, coldDc = 18, weatherEventDc = 18),
)

@JsPlainObject
external interface ClimateSetting {
    val coldDc: Int?
    val precipitationDc: Int?
    val season: String
    val weatherEventDc: Int?
}

@JsPlainObject
external interface ClimateSettings {
    var months: Array<ClimateSetting>
}

@Suppress("unused")
@JsPlainObject
external interface ClimateSettingsContext : ValidatedHandlebarsContext {
    var heading: Array<TableHead>
    var formRows: Array<Array<Any>>
}

@JsExport
class ClimateConfigurationDataModel(
    value: AnyObject? = undefined,
    context: DocumentConstructionContext? = undefined,
) : DataModel(value, context) {
    companion object {
        @JsStatic
        fun defineSchema() = buildSchema {
            array("months") {
                options {
                    initial = getDefaultMonths()
                }
                schema {
                    int("coldDc", nullable = true)
                    int("precipitationDc", nullable = true)
                    int("weatherEventDc", nullable = true)
                    string("season")
                }
            }
        }
    }
}

@JsExport
class ClimateConfiguration : FormApp<ClimateSettingsContext, ClimateSettings>(
    title = "Climate",
    width = 1024,
    template = "applications/settings/configure-climate.hbs",
    debug = true,
    dataModel = ClimateConfigurationDataModel::class.js,
    id = "kmClimate",
) {
    private var currentSettings = game.settings.pfrpg2eKingdomCampingWeather.getClimateSettings()

    override fun _onClickAction(event: PointerEvent, target: HTMLElement) {
        when (val action = target.dataset["action"]) {
            "km-save" -> {
                buildPromise {
                    game.settings.pfrpg2eKingdomCampingWeather.setClimateSettings(currentSettings)
                    close()
                }
            }

            else -> console.log(action)
        }
    }

    override fun _preparePartContext(
        partId: String,
        context: HandlebarsRenderContext,
        options: HandlebarsRenderOptions
    ): Promise<ClimateSettingsContext> = buildPromise {
        val parent = super._preparePartContext(partId, context, options).await()
        ClimateSettingsContext(
            partId = parent.partId,
            isFormValid = isFormValid,
            heading = arrayOf(
                TableHead(t("enums.month")),
                TableHead(t("enums.season")),
                TableHead(t("weather.coldDc"), arrayOf("number-select-heading")),
                TableHead(t("weather.precipitationDc"), arrayOf("number-select-heading")),
                TableHead(t("weather.weatherEventDc"), arrayOf("number-select-heading")),
            ),
            formRows = currentSettings.months.mapIndexed { index, row ->
                val month = Month.entries[index]
                arrayOf(
                    t(month),
                    Select.fromEnum<Season>(
                        name = "months.$index.season",
                        value = Season.entries.find { it.toCamelCase() == row.season },
                        hideLabel = true
                    ).toContext(),
                    Select.flatCheck(
                        name = "months.$index.coldDc",
                        label = t("weather.coldDc"),
                        value = row.coldDc,
                        hideLabel = true,
                        required = false,
                    ).toContext(),
                    Select.flatCheck(
                        name = "months.$index.precipitationDc",
                        label = t("weather.precipitationDc"),
                        value = row.precipitationDc,
                        hideLabel = true,
                        required = false,
                    ).toContext(),
                    Select.flatCheck(
                        name = "months.$index.weatherEventDc",
                        label = t("weather.weatherEventDc"),
                        value = row.weatherEventDc,
                        hideLabel = true,
                        required = false,
                    ).toContext(),
                )
            }.toTypedArray()
        )
    }

    override fun fixObject(value: dynamic) {
        value["months"] = (value["months"] as Array<ClimateSetting>?) ?: getDefaultMonths()
    }

    override fun onParsedSubmit(value: ClimateSettings) = buildPromise {
        currentSettings = value
        null
    }
}