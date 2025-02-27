import java.util.ArrayList;

class MiniMax {
    private final Mark cpuMark;
    private final Mark humanMark;
    private final BoardEvaluator evaluator;
    private final int maxDepth;

    public MiniMax(Mark cpuMark, int maxDepth) {
        this.cpuMark = cpuMark;
        this.humanMark = (cpuMark == Mark.X) ? Mark.O : Mark.X;
        this.evaluator = new BoardEvaluator(humanMark); // Evaluator from human perspective as in JS
        this.maxDepth = maxDepth;
    }

    public Move findBestMove(Board board) {
        ArrayList<Move> availableMoves = board.getAvailableMoves();
        if (availableMoves.isEmpty()) {
            return null;
        }

        Move bestMove = null;
        int bestScore = Integer.MAX_VALUE; // We're minimizing (CPU is O in the JS)

        for (Move move : availableMoves) {
            // Try this move
            Board newBoard = board.copy();
            newBoard.play(move, cpuMark);

            // Get score from minimax
            int score = minimax(newBoard, move, humanMark, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

            // Update best move if needed (minimizing)
            if (score < bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private int minimax(Board board, Move lastMove, Mark player, int depth, int alpha, int beta) {
        // Evaluate position if terminal or max depth reached
        if (depth == maxDepth || board.isGameOver()) {
            return evaluator.evaluate(board, lastMove.getGlobalIndex());
        }

        // Get available moves
        ArrayList<Move> availableMoves = board.getAvailableMoves();
        if (availableMoves.isEmpty()) {
            return evaluator.evaluate(board, lastMove.getGlobalIndex());
        }

        if (player == humanMark) { // Maximizing player (X in the JS)
            int maxScore = Integer.MIN_VALUE;

            for (Move move : availableMoves) {
                Board newBoard = board.copy();
                newBoard.play(move, player);

                int score = minimax(newBoard, move, cpuMark, depth + 1, alpha, beta);
                maxScore = Math.max(maxScore, score);

                alpha = Math.max(alpha, maxScore);
                if (beta <= alpha) {
                    break; // Beta cutoff
                }
            }

            return maxScore;
        } else { // Minimizing player (O in the JS)
            int minScore = Integer.MAX_VALUE;

            for (Move move : availableMoves) {
                Board newBoard = board.copy();
                newBoard.play(move, player);

                int score = minimax(newBoard, move, humanMark, depth + 1, alpha, beta);
                minScore = Math.min(minScore, score);

                beta = Math.min(beta, minScore);
                if (beta <= alpha) {
                    break; // Alpha cutoff
                }
            }

            return minScore;
        }
    }
}