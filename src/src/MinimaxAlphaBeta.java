import java.util.List;

/**
 * Implements the Minimax algorithm with Alpha-Beta pruning for the Ultimate Tic-Tac-Toe game
 */
public class MinimaxAlphaBeta {
    private static final int MAX_DEPTH = 12;
    private static long timeLimit;
    private static long startTime;
    private static boolean timeLimitReached;

    // Find the best move for the player using minimax with alpha-beta pruning
    public static Move findBestMove(Board board, int player, long timeLimitMillis) {
        timeLimit = timeLimitMillis;
        startTime = System.currentTimeMillis();
        timeLimitReached = false;

        // Start with a low depth and increase it until time runs out (iterative deepening)
        Move bestMove = null;
        Move lastCompletedMove = null;

        // Always try at least depth 1 to get any valid move
        try {
            bestMove = findBestMoveAtDepth(board, player, 1);
            lastCompletedMove = bestMove;
        } catch (TimeoutException e) {
            System.out.println("Timeout reached at depth 1");
            return bestMove;
        }

        // Now search deeper with the remaining time
        for (int depth = 2; depth <= MAX_DEPTH; depth++) {
            try {
                long elapsedTime = System.currentTimeMillis() - startTime;
                long remainingTime = timeLimit - elapsedTime;

                // If we have less than 10% of total time remaining, stop searching deeper
                if (remainingTime < (timeLimit * 0.1)) {
                    System.out.println("Not enough time for depth " + depth + ", stopping search");
                    break;
                }

                Move move = findBestMoveAtDepth(board, player, depth);
                if (!timeLimitReached) {
                    lastCompletedMove = move;
                    bestMove = move; // Update best move
                    System.out.println("Completed search at depth " + depth);
                } else {
                    break;
                }
            } catch (TimeoutException e) {
                System.out.println("Timeout reached at depth " + depth);
                break;
            }
        }

        // If we have a completed move, return that, otherwise return the best move we have
        if (lastCompletedMove != null) {
            bestMove = lastCompletedMove;
        }

        // If we still have a lot of time left, let's deliberately use more of it
        long elapsedTime = System.currentTimeMillis() - startTime;
        long remainingTime = timeLimit - elapsedTime;

        // If we have more than 30% of our time left, let's use some of it
        if (remainingTime > (timeLimit * 0.3) && remainingTime > 200) {
            long sleepTime = Math.min(remainingTime - 100, 1000); // Sleep for up to 1 second, leaving at least 100ms
            try {
                System.out.println("Thinking more deeply for " + sleepTime + "ms");
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return bestMove;
    }

    // Find the best move at a specific depth
    private static Move findBestMoveAtDepth(Board board, int player, int depth) throws TimeoutException {
        List<Move> possibleMoves = MoveGenerator.generateMoves(board);
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Move move : possibleMoves) {
            // Check if time limit has been reached
            if (System.currentTimeMillis() - startTime > timeLimit * 0.95) { // Use 95% of time limit for search
                timeLimitReached = true;
                throw new TimeoutException();
            }

            // Make the move
            Board newBoard = new Board(board);
            newBoard.makeMove(move.getRow(), move.getCol(), player);

            // Evaluate the move
            int score = minimax(newBoard, depth - 1, alpha, beta, false, player);

            // Update best move if needed
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            // Update alpha
            alpha = Math.max(alpha, bestScore);
        }

        return bestMove;
    }

    // Minimax algorithm with alpha-beta pruning
    private static int minimax(Board board, int depth, int alpha, int beta, boolean isMaximizing, int player) throws TimeoutException {
        // Check if time limit has been reached
        if (System.currentTimeMillis() - startTime > timeLimit * 0.95) {
            timeLimitReached = true;
            throw new TimeoutException();
        }

        int opponent = (player == 4) ? 2 : 4;
        int gameStatus = board.checkGameStatus();

        // Check if the game is over or we've reached the maximum depth
        if (gameStatus != 0 || depth == 0) {
            return Evaluator.evaluate(board, player);
        }

        List<Move> possibleMoves = MoveGenerator.generateMoves(board);

        // If no moves available, evaluate the current position
        if (possibleMoves.isEmpty()) {
            return Evaluator.evaluate(board, player);
        }

        if (isMaximizing) {
            int bestScore = Integer.MIN_VALUE;

            for (Move move : possibleMoves) {
                // Make the move
                Board newBoard = new Board(board);
                newBoard.makeMove(move.getRow(), move.getCol(), player);

                // Recursively evaluate the move
                int score = minimax(newBoard, depth - 1, alpha, beta, false, player);

                // Update best score
                bestScore = Math.max(bestScore, score);

                // Update alpha
                alpha = Math.max(alpha, bestScore);

                // Alpha-beta pruning
                if (beta <= alpha) {
                    break;
                }
            }

            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;

            for (Move move : possibleMoves) {
                // Make the move
                Board newBoard = new Board(board);
                newBoard.makeMove(move.getRow(), move.getCol(), opponent);

                // Recursively evaluate the move
                int score = minimax(newBoard, depth - 1, alpha, beta, true, player);

                // Update best score
                bestScore = Math.min(bestScore, score);

                // Update beta
                beta = Math.min(beta, bestScore);

                // Alpha-beta pruning
                if (beta <= alpha) {
                    break;
                }
            }

            return bestScore;
        }
    }

    // Exception to handle timeout
    private static class TimeoutException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}