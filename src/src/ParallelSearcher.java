import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Implements parallel search for the first level of the minimax tree
 */
public class ParallelSearcher {
    // Maximum number of threads to use
    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

    // Transposition table and Zobrist hash for optimization
    private TranspositionTable transpositionTable;
    private ZobristHash zobristHash;

    // Thread pool for running parallel searches
    private ExecutorService executor;

    /**
     * Constructor initializes the thread pool and other components
     */
    public ParallelSearcher() {
        transpositionTable = new TranspositionTable();
        zobristHash = new ZobristHash();
        executor = Executors.newFixedThreadPool(MAX_THREADS);
    }

    /**
     * Find the best move in parallel for the given board position
     *
     * @param board The current board position
     * @param player The current player (4 for X, 2 for O)
     * @param depth The search depth
     * @param timeLimit Time limit in milliseconds
     * @return The best move found
     */
    public Move findBestMove(Board board, int player, int depth, long timeLimit) {
        long startTime = System.currentTimeMillis();
        List<Move> possibleMoves = MoveGenerator.generateMoves(board);

        // If there's only one move, no need to search
        if (possibleMoves.size() == 1) {
            return possibleMoves.get(0);
        }

        // Sort moves to try the most promising ones first
        possibleMoves = sortMoves(board, possibleMoves, player);

        // Create search tasks
        List<SearchTask> tasks = new ArrayList<>();
        for (Move move : possibleMoves) {
            SearchTask task = new SearchTask(board, move, player, depth, startTime, timeLimit);
            tasks.add(task);
        }

        try {
            // Submit tasks for execution
            List<Future<MoveScore>> results = executor.invokeAll(tasks,
                    timeLimit - 100, TimeUnit.MILLISECONDS);

            // Find the best move from the completed results
            Move bestMove = null;
            int bestScore = Integer.MIN_VALUE;

            for (int i = 0; i < results.size(); i++) {
                Future<MoveScore> future = results.get(i);
                if (future.isDone() && !future.isCancelled()) {
                    try {
                        MoveScore moveScore = future.get();
                        if (moveScore.score > bestScore) {
                            bestScore = moveScore.score;
                            bestMove = moveScore.move;
                        }
                    } catch (Exception e) {
                        // Handle exception: use the move without evaluation
                        if (bestMove == null) {
                            bestMove = possibleMoves.get(i);
                        }
                    }
                }
            }

            // Return the best move found, or the first move if no evaluation completed
            return bestMove != null ? bestMove : possibleMoves.get(0);

        } catch (InterruptedException e) {
            // If interrupted, return the first move as a fallback
            return possibleMoves.get(0);
        }
    }

    /**
     * Sort moves to evaluate the most promising ones first
     * This improves alpha-beta pruning efficiency
     */
    private List<Move> sortMoves(Board board, List<Move> moves, int player) {
        // Check if we have any stored best moves for this position
        long boardHash = zobristHash.computeHash(board);
        Move bestMove = transpositionTable.getBestMove(boardHash);

        // Prioritize the stored best move if available
        if (bestMove != null) {
            moves.remove(bestMove);
            moves.add(0, bestMove);
        }

        // Could add more sophisticated move ordering based on heuristics
        // For example, prioritize moves in the center of local boards

        return moves;
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * A task that searches for the best score for a specific move
     */
    private class SearchTask implements Callable<MoveScore> {
        private Board board;
        private Move move;
        private int player;
        private int depth;
        private long startTime;
        private long timeLimit;

        public SearchTask(Board board, Move move, int player, int depth, long startTime, long timeLimit) {
            this.board = board;
            this.move = move;
            this.player = player;
            this.depth = depth;
            this.startTime = startTime;
            this.timeLimit = timeLimit;
        }

        @Override
        public MoveScore call() throws Exception {
            // Create a copy of the board and make the move
            Board newBoard = new Board(board);
            newBoard.makeMove(move.getRow(), move.getCol(), player);

            // Apply minimax search from this position
            int score = minimax(newBoard, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE,
                    false, player, startTime, timeLimit);

            return new MoveScore(move, score);
        }

        /**
         * Minimax algorithm with alpha-beta pruning and transposition table
         */
        private int minimax(Board board, int depth, int alpha, int beta, boolean isMaximizing,
                            int player, long startTime, long timeLimit) throws TimeoutException {

            // Check time limit periodically
            if (depth % 3 == 0) {
                checkTimeLimit(startTime, timeLimit);
            }

            int opponent = (player == 4) ? 2 : 4;
            int gameStatus = board.checkGameStatus();

            // Terminal conditions
            if (gameStatus != 0 || depth == 0) {
                return Evaluator.evaluate(board, player);
            }

            // Check transposition table
            long boardHash = zobristHash.computeHash(board);
            Integer cachedScore = transpositionTable.probe(boardHash, depth, alpha, beta);
            if (cachedScore != null) {
                return cachedScore;
            }

            List<Move> possibleMoves = MoveGenerator.generateMoves(board);

            // If no moves available, evaluate current position
            if (possibleMoves.isEmpty()) {
                return Evaluator.evaluate(board, player);
            }

            // Sort moves for better pruning
            possibleMoves = sortMoves(board, possibleMoves, isMaximizing ? player : opponent);

            int bestScore;
            int nodeType = TranspositionTable.UPPER_BOUND;
            Move bestMove = null;

            if (isMaximizing) {
                bestScore = Integer.MIN_VALUE;

                for (Move move : possibleMoves) {
                    Board newBoard = new Board(board);
                    newBoard.makeMove(move.getRow(), move.getCol(), player);

                    int score = minimax(newBoard, depth - 1, alpha, beta, false, player, startTime, timeLimit);

                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = move;
                    }

                    alpha = Math.max(alpha, bestScore);

                    if (beta <= alpha) {
                        break;
                    }
                }

                nodeType = bestScore <= alpha ? TranspositionTable.UPPER_BOUND :
                        (bestScore >= beta ? TranspositionTable.LOWER_BOUND : TranspositionTable.EXACT);

            } else {
                bestScore = Integer.MAX_VALUE;

                for (Move move : possibleMoves) {
                    Board newBoard = new Board(board);
                    newBoard.makeMove(move.getRow(), move.getCol(), opponent);

                    int score = minimax(newBoard, depth - 1, alpha, beta, true, player, startTime, timeLimit);

                    if (score < bestScore) {
                        bestScore = score;
                        bestMove = move;
                    }

                    beta = Math.min(beta, bestScore);

                    if (beta <= alpha) {
                        break;
                    }
                }

                nodeType = bestScore <= alpha ? TranspositionTable.UPPER_BOUND :
                        (bestScore >= beta ? TranspositionTable.LOWER_BOUND : TranspositionTable.EXACT);
            }

            // Store result in transposition table
            transpositionTable.store(boardHash, bestScore, depth, nodeType, bestMove);

            return bestScore;
        }

        /**
         * Check if time limit has been reached
         */
        private void checkTimeLimit(long startTime, long timeLimit) throws TimeoutException {
            if (System.currentTimeMillis() - startTime > timeLimit * 0.95) {
                throw new TimeoutException();
            }
        }
    }

    /**
     * A simple class to hold a move and its score
     */
    private static class MoveScore {
        Move move;
        int score;

        public MoveScore(Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }

    /**
     * Exception for time limit exceeded
     */
    private static class TimeoutException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}