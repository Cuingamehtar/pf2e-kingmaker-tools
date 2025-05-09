package at.posselt.pfrpg2e.data.kingdom.structures

data class AvailableItemsRule(
    val value: Int,
    val group: ItemGroup? = null,
    val maximumStacks: Int = 3,
    val alwaysStacks: Boolean = false,
)