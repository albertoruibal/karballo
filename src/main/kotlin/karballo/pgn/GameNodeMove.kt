package karballo.pgn

class GameNodeMove(var number: String, var move: String, var annotation: String) : GameNode() {

    override fun toString(): String {
        return "GameNodeMove{" +
                "number='" + number + '\'' +
                ", move='" + move + '\'' +
                ", annotation='" + annotation + '\'' +
                "}\n"
    }
}
