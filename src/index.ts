import {dayHasChanged, syncWeather, toggleWeather} from './weather';
import {isGm} from './utils';
import {toTimeOfDayMacro} from './time/app';
import {getNumberSetting, getRollMode} from "./settings";
import {rollWeather} from "./kingmaker-weather";
import {randomEncounterDialog} from "./random-encounters";
import {rollExplorationSkillCheck, rollSkillDialog} from "./skill-checks";

Hooks.on('ready', async () => {
    if (game instanceof Game) {
        const gameInstance = game;
        gameInstance.pf2eKingmakerTools = {
            macros: {
                toggleWeatherMacro: toggleWeather.bind(null, game),
                toTimeOfDayMacro: toTimeOfDayMacro.bind(null, game),
                randomEncounterMacro: randomEncounterDialog.bind(null, game),
                rollExplorationSkillCheck: async (skill: string, effect: string) => {
                    const actors = canvas?.scene?.tokens
                        ?.filter(t => t !== null && t.actor !== null && t.actor.type === 'character')
                        ?.map(t => t.actor!) ?? [];
                    await rollExplorationSkillCheck(actors, skill, effect);
                },
                rollSkillDialog: async (actorNames: Set<string>) => {
                    const actors = gameInstance?.actors
                        ?.filter(actor => actor.type === 'character'
                            && actor.name !== null
                            && actorNames.has(actor.name)) ?? [];
                    await rollSkillDialog(actors);
                },
            },
        };
        const rollModeChoices = {
            publicroll: 'Public Roll',
            gmroll: 'Private GM Roll',
            blindroll: 'Blind GM Roll',
            selfroll: 'Self Roll',
        };
        gameInstance.settings.register<string, string, number>('pf2e-kingmaker-tools', 'averagePartyLevel', {
            name: 'Average Party Level',
            default: 1,
            config: true,
            type: Number,
            scope: 'world',
        });
        gameInstance.settings.register<string, string, boolean>('pf2e-kingmaker-tools', 'enableWeather', {
            name: 'Enable Weather',
            default: true,
            config: true,
            type: Boolean,
            scope: 'world',
        });
        gameInstance.settings.register<string, string, string>('pf2e-kingmaker-tools', 'weatherRollMode', {
            name: 'Weather Roll Mode',
            scope: 'world',
            config: true,
            default: 'gmroll',
            type: String,
            choices: rollModeChoices,
        });
        gameInstance.settings.register('pf2e-kingmaker-tools', 'currentWeatherFx', {
            name: 'Current Weather FX',
            hint: 'Based on the current value of the roll table',
            scope: 'world',
            config: true,
            default: 'sunny',
            type: String,
        });
        gameInstance.settings.register('pf2e-kingmaker-tools', 'currentRegion', {
            name: 'Current Region',
            hint: 'Region used for random encounters',
            scope: 'world',
            config: true,
            default: 'Rostland',
            type: String,
        });
        gameInstance.settings.register('pf2e-kingmaker-tools', 'currentEncounterDCModifier', {
            name: 'Current Encounter DC Modifier',
            scope: 'world',
            config: true,
            default: 0,
            type: Number,
        });
        Hooks.on('updateWorldTime', async (_, delta) => {
            if (isGm(gameInstance) && dayHasChanged(gameInstance, delta)) {
                const rollMode = getRollMode(gameInstance, 'weatherRollMode');
                const averagePartyLevel = getNumberSetting(gameInstance, 'averagePartyLevel');
                await rollWeather(gameInstance, averagePartyLevel, rollMode);
            }
        });
        Hooks.on('canvasReady', async () => {
            if (isGm(gameInstance)) {
                await syncWeather(gameInstance);
            }
        });
    }
});
