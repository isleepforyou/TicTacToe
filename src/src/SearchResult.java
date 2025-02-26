import java.util.ArrayList;

public class SearchResult {
    private ArrayList<Move> bestMoves;
    private int bestScore;
    private int maxDepthReached;

    public SearchResult(ArrayList<Move> bestMoves, int bestScore, int maxDepthReached) {
        this.bestMoves = bestMoves;
        this.bestScore = bestScore;
        this.maxDepthReached = maxDepthReached;
    }

    public ArrayList<Move> getBestMoves() {
        return bestMoves;
    }

    public int getBestScore() {
        return bestScore;
    }

    public int getMaxDepthReached() {
        return maxDepthReached;
    }
}