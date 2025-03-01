package at.posselt.pfrpg2e.data.kingdom.structures

import at.posselt.pfrpg2e.data.kingdom.KingdomSkill

data class StructureActivityBonus(
    val structureNames: List<String>,
    val skill: KingdomSkill,
    val activity: String,
    val value: Int,
)