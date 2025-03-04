package at.posselt.pfrpg2e.kingdom.modifiers.expressions

data class Gte(val left: Any?, val right: Any?) : Expression<Boolean> {
    override fun evaluate(context: ExpressionContext): Boolean =
        context.evaluateInt(left) >= context.evaluateInt(right)
}