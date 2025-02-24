import {showKingdom} from './kingdom/sheet';
import {getKingdom} from './kingdom/storage';
import {addOngoingEvent, changeDegree, parseUpgradeMeta, reRoll} from './kingdom/rolls';
import {kingdomChatButtons} from './kingdom/chat-buttons';
import {StringDegreeOfSuccess} from './degree-of-success';
import {updateKingdomArmyConsumption} from './armies/utils';
import {structureTokenMappingDialog} from './kingdom/dialogs/structure-token-mapping-dialog';
import {showStructureHints} from './kingdom/structures';


Hooks.on('ready', async () => {
    if (game instanceof Game) {
        const gameInstance = game;
        gameInstance.pf2eKingmakerTools.macros.structureTokenMappingMacro = structureTokenMappingDialog.bind(null, game);
        gameInstance.pf2eKingmakerTools.macros.viewKingdomMacro = showKingdom.bind(null, game);
        // hooks
        // army consumption
        const updateConsumption = async (actor: Actor | null): Promise<void> => {
            await updateKingdomArmyConsumption({
                actor,
                kingdomActor: getKingdomSheetActor(gameInstance),
                game: gameInstance,
            });
        };
        const forceUpdateConsumption = async (): Promise<void> => {
            await updateKingdomArmyConsumption({
                forceUpdate: true,
                kingdomActor: getKingdomSheetActor(gameInstance),
                game: gameInstance,
            });
        };
        Hooks.on('createToken', (token: StoredDocument<Token>) => {
            showStructureHints(game, token.actor);
            updateConsumption(token.actor);
        });
        Hooks.on('updateToken', (token: StoredDocument<Token>) => updateConsumption(token.actor));
        Hooks.on('deleteToken', (token: StoredDocument<Token>) => updateConsumption(token.actor));
        Hooks.on('updateActor', (actor: StoredDocument<Actor>) => updateConsumption(actor));
        Hooks.on('updateItem', (item: StoredDocument<Item>) => updateConsumption(item.actor));
        Hooks.on('createItem', (item: StoredDocument<Item>) => updateConsumption(item.actor));
        Hooks.on('deleteItem', (item: StoredDocument<Item>) => updateConsumption(item.actor));
        Hooks.on('deleteScene', () => forceUpdateConsumption());
        checkKingdomErrors(gameInstance);

        // listen for camping sheet open
        gameInstance.socket!.on('module.pf2e-kingmaker-tools', async (data: { action: string, data: any }) => {
            if (data.action === 'openKingdomSheet') {
                await showKingdom(gameInstance);
            }
        });
    }
});

Hooks.on('init', async () => {
    await loadTemplates([
        "modules/pf2e-kingmaker-tools/templates/common/skills.partial.hbs",
        'modules/pf2e-kingmaker-tools/templates/kingdom/structure-browser-item.hbs',
        'modules/pf2e-kingmaker-tools/templates/kingdom/sidebar.hbs',
        'modules/pf2e-kingmaker-tools/templates/kingdom/status.hbs',
        'modules/pf2e-kingmaker-tools/templates/kingdom/skills.hbs',
        'modules/pf2e-kingmaker-tools/templates/kingdom/turn.hbs',
        'modules/pf2e-kingmaker-tools/templates/kingdom/groups.hbs',
        'modules/pf2e-kingmaker-tools/templates/kingdom/feats.hbs',
        'modules/pf2e-kingmaker-tools/templates/kingdom/notes.hbs',
        'modules/pf2e-kingmaker-tools/templates/kingdom/settlements.hbs',
        'modules/pf2e-kingmaker-tools/templates/kingdom/settlement.hbs',
        'modules/pf2e-kingmaker-tools/templates/kingdom/effects.hbs',
    ]);
});

Hooks.on('renderChatLog', () => {
    if (game instanceof Game) {
        const gameInstance = game;
        const chatLog = $('#chat-log');
        for (const button of kingdomChatButtons) {
            chatLog.on('click', button.selector, (event: Event) => {
                const actor = getKingdomSheetActor(gameInstance);
                if (actor) {
                    return button.callback(gameInstance, actor, event);
                }
            });
        }
    }
});

type LogEntry = {
    name: string,
    icon?: string,
    condition?: (html: JQuery) => boolean,
    callback: (html: JQuery) => void,
};

function getKingdomSheetActor(game: Game): Actor | undefined {
    return game?.actors?.find(a => a.name === 'Kingdom Sheet');
}

function ifKingdomActorExists(game: Game, el: HTMLElement, callback: (actor: Actor) => void): void {
    // TODO: replace this with a data-actor-id lookup in the el
    const actor = getKingdomSheetActor(game);
    if (actor) {
        callback(actor);
    } else {
        ui.notifications?.error('Could not find actor with name Kingdom Sheet');
    }
}

Hooks.on('getChatLogEntryContext', (html: HTMLElement, items: LogEntry[]) => {
    if (game instanceof Game) {
        const gameInstance = game;
        const hasActor = (): boolean => getKingdomSheetActor(gameInstance) !== undefined;
        const hasContentLink = (el: JQuery): boolean => hasActor() && el[0].querySelector('.content-link') !== null;
        const hasMeta = (el: JQuery): boolean => hasActor() && el[0].querySelector('.km-roll-meta') !== null;
        const canChangeDegree = (chatMessage: HTMLElement, direction: 'upgrade' | 'downgrade'): boolean => {
            const cantChangeDegreeUnless: StringDegreeOfSuccess = direction === 'upgrade' ? 'criticalSuccess' : 'criticalFailure';
            const meta = chatMessage.querySelector('.km-upgrade-result');
            return hasActor() && meta !== null && parseUpgradeMeta(chatMessage).degree !== cantChangeDegreeUnless;
        };
        const canReRollUsingFame = (el: JQuery): boolean => {
            const actor = getKingdomSheetActor(gameInstance);
            return actor ? getKingdom(actor).fame.now > 0 && hasMeta(el) : false;
        };
        const canReRollUsingCreativeSolution = (el: JQuery): boolean => {
            const actor = getKingdomSheetActor(gameInstance);
            return actor ? getKingdom(actor).creativeSolutions > 0 && hasMeta(el) : false;
        };
        const canReRollUsingSupernaturalSolution = (el: JQuery): boolean => {
            const actor = getKingdomSheetActor(gameInstance);
            return actor ? getKingdom(actor).supernaturalSolutions > 0 && hasMeta(el) : false;
        };
        items.push({
            name: 'Re-Roll Using Fame/Infamy',
            icon: '<i class="fa-solid fa-dice-d20"></i>',
            condition: canReRollUsingFame,
            callback: el => ifKingdomActorExists(gameInstance, el[0], (actor) => reRoll(actor, el[0], 'fame')),
        }, {
            name: 'Re-Roll Using Creative Solution',
            icon: '<i class="fa-solid fa-dice-d20"></i>',
            condition: canReRollUsingCreativeSolution,
            callback: el => ifKingdomActorExists(gameInstance, el[0], (actor) => reRoll(actor, el[0], 'creative-solution')),
        }, {
            name: 'Re-Roll Using Supernatural Solution',
            icon: '<i class="fa-solid fa-dice-d20"></i>',
            condition: canReRollUsingSupernaturalSolution,
            callback: el => ifKingdomActorExists(gameInstance, el[0], (actor) => reRoll(actor, el[0], 'supernatural-solution')),
        }, {
            name: 'Re-Roll',
            condition: hasMeta,
            icon: '<i class="fa-solid fa-dice-d20"></i>',
            callback: el => ifKingdomActorExists(gameInstance, el[0], (actor) => reRoll(actor, el[0], 're-roll')),
        }, {
            name: 'Re-Roll Keep Higher',
            condition: hasMeta,
            icon: '<i class="fa-solid fa-dice-d20"></i>',
            callback: el => ifKingdomActorExists(gameInstance, el[0], (actor) => reRoll(actor, el[0], 'keep-higher')),
        }, {
            name: 'Re-Roll Keep Lower',
            condition: hasMeta,
            icon: '<i class="fa-solid fa-dice-d20"></i>',
            callback: el => ifKingdomActorExists(gameInstance, el[0], (actor) => reRoll(actor, el[0], 'keep-lower')),
        }, {
            name: 'Upgrade Degree of Success',
            condition: (el: JQuery) => canChangeDegree(el[0], 'upgrade'),
            icon: '<i class="fa-solid fa-arrow-up"></i>',
            callback: el => ifKingdomActorExists(gameInstance, el[0], (actor) => changeDegree(actor, el[0], 'upgrade')),
        }, {
            name: 'Downgrade Degree of Success',
            condition: (el: JQuery) => canChangeDegree(el[0], 'downgrade'),
            icon: '<i class="fa-solid fa-arrow-down"></i>',
            callback: el => ifKingdomActorExists(gameInstance, el[0], (actor) => changeDegree(actor, el[0], 'downgrade')),
        }, {
            name: 'Add to Ongoing Events',
            icon: '<i class="fa-solid fa-plus"></i>',
            condition: hasContentLink,
            callback: async (el) => {
                ifKingdomActorExists(gameInstance, el[0], async (actor) => {
                    const link = el[0].querySelector('.content-link') as HTMLElement | null;
                    const uuid = link?.dataset?.uuid;
                    const text = link?.innerText;
                    if (uuid && text) {
                        await addOngoingEvent(actor, uuid, text);
                    }
                });
            },
        });
    }
});

function checkKingdomErrors(game: Game): void {
    const actor = getKingdomSheetActor(game);
    if (actor && !actor.getFlag('pf2e-kingmaker-tools', 'kingdom-sheet')) {
        ui.notifications?.error('Found an Actor with name "Kingdom Sheet" that has not been imported using the "View Kingdom" Macro! Please delete the actor and re-import it.');
    }
}