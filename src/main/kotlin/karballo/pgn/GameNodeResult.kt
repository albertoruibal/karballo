package karballo.pgn

class GameNodeResult(var result: String) : GameNode() {

    override fun toString(): String {
        return "GameNodeResult{" +
                "result='" + result + '\'' +
                "}\n"
    }
}
