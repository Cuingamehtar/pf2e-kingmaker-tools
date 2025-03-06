package at.posselt.pfrpg2e.kingdom.modifiers.penalties

import at.posselt.pfrpg2e.data.checks.DegreeOfSuccess
import at.posselt.pfrpg2e.kingdom.modifiers.DowngradeResult
import at.posselt.pfrpg2e.kingdom.modifiers.Modifier
import at.posselt.pfrpg2e.kingdom.modifiers.ModifierType
import at.posselt.pfrpg2e.kingdom.modifiers.expressions.Gte

fun createAnarchyPenalty(anarchyLimit: Int) =
    Modifier(
        id = "anarchy",
        type = ModifierType.UNTYPED,
        value = 0,
        name = "Anarch worsens all checks by 1 degree",
        applyIf = listOf(
            Gte("@unrest", anarchyLimit)
        ),
        downgradeResults = listOf(
            DowngradeResult(
                downgrade = DegreeOfSuccess.CRITICAL_SUCCESS,
            ),
            DowngradeResult(
                downgrade = DegreeOfSuccess.SUCCESS,
            ),
            DowngradeResult(
                downgrade = DegreeOfSuccess.FAILURE,
            )
        )
    )