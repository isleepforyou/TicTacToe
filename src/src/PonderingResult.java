import java.util.ArrayList;

public class PonderingResult {
    private ArrayList<Move> bestMoves;
    private int bestScore;
    private int depth;

    public PonderingResult(ArrayList<Move> bestMoves, int bestScore, int depth) {
        this.bestMoves = bestMoves;
        this.bestScore = bestScore;
        this.depth = depth;
    }

    public ArrayList<Move> getBestMoves() {
        return bestMoves;
    }

    public int getBestScore() {
        return bestScore;
    }

    public int getDepth() {
        return depth;
    }
}