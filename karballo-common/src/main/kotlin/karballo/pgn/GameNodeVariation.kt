package karballo.pgn

class GameNodeVariation : GameNode() {

    var variation = ArrayList<GameNode>()

    fun add(gameNode: GameNode) {
        variation.add(gameNode)
    }

    operator fun get(index: Int): GameNode {
        return variation[index]
    }

    fun size(): Int {
        return variation.size
    }

    override fun toString(): String {
        return "GameNodeVariation{" +
                "variation=" + variation +
                "}\n"
    }
}
