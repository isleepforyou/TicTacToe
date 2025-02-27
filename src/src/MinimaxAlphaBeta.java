import java.util.List;

/**
 * Algorithme Minimax avec élagage Alpha-Beta pour Ultimate Tic-Tac-Toe
 */
public class MinimaxAlphaBeta {
    private static final int MAX_DEPTH = 12;
    private static long timeLimit;
    private static long startTime;
    private static boolean timeLimitReached;

    // Trouve le meilleur coup
    public static Move findBestMove(Board board, int player, long timeLimitMillis) {
        timeLimit = timeLimitMillis;
        startTime = System.currentTimeMillis();
        timeLimitReached = false;

        // Augmente la profondeur progressivement (approfondissement itératif)
        Move bestMove = null;
        Move lastCompletedMove = null;

        // Profondeur 1 minimum
        try {
            bestMove = findBestMoveAtDepth(board, player, 1);
            lastCompletedMove = bestMove;
        } catch (TimeoutException e) {
            System.out.println("Timeout reached at depth 1");
            return bestMove;
        }

        // Recherche plus profonde avec le temps restant
        for (int depth = 2; depth <= MAX_DEPTH; depth++) {
            try {
                long elapsedTime = System.currentTimeMillis() - startTime;
                long remainingTime = timeLimit - elapsedTime;

                // Arrête si moins de 10% du temps total reste
                if (remainingTime < (timeLimit * 0.1)) {
                    System.out.println("Not enough time for depth " + depth + ", stopping search");
                    break;
                }

                Move move = findBestMoveAtDepth(board, player, depth);
                if (!timeLimitReached) {
                    lastCompletedMove = move;
                    bestMove = move;
                    System.out.println("Completed search at depth " + depth);
                } else {
                    break;
                }
            } catch (TimeoutException e) {
                System.out.println("Timeout reached at depth " + depth);
                break;
            }
        }

        // Retourne le coup complet ou le meilleur disponible
        if (lastCompletedMove != null) {
            bestMove = lastCompletedMove;
        }

        // Utilise le temps restant si beaucoup est disponible
        long elapsedTime = System.currentTimeMillis() - startTime;
        long remainingTime = timeLimit - elapsedTime;

        // Si plus de 30% du temps reste
        if (remainingTime > (timeLimit * 0.3) && remainingTime > 200) {
            long sleepTime = Math.min(remainingTime - 100, 1000);
            try {
                System.out.println("Thinking more deeply for " + sleepTime + "ms");
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return bestMove;
    }

    // Trouve le meilleur coup à une profondeur spécifique
    private static Move findBestMoveAtDepth(Board board, int player, int depth) throws TimeoutException {
        List<Move> possibleMoves = MoveGenerator.generateMoves(board);
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Move move : possibleMoves) {
            // Vérifie si limite de temps atteinte
            if (System.currentTimeMillis() - startTime > timeLimit * 0.95) {
                timeLimitReached = true;
                throw new TimeoutException();
            }

            // Joue le coup
            Board newBoard = new Board(board);
            newBoard.makeMove(move.getRow(), move.getCol(), player);

            // Évalue le coup
            int score = minimax(newBoard, depth - 1, alpha, beta, false, player);

            // Met à jour le meilleur coup si nécessaire
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            // Met à jour alpha
            alpha = Math.max(alpha, bestScore);
        }

        return bestMove;
    }

    // Algorithme minimax avec élagage alpha-beta
    private static int minimax(Board board, int depth, int alpha, int beta, boolean isMaximizing, int player) throws TimeoutException {
        // Vérifie limite de temps
        if (System.currentTimeMillis() - startTime > timeLimit * 0.95) {
            timeLimitReached = true;
            throw new TimeoutException();
        }

        int opponent = (player == 4) ? 2 : 4;
        int gameStatus = board.checkGameStatus();

        // Vérifie fin de partie ou profondeur max
        if (gameStatus != 0 || depth == 0) {
            return Evaluator.evaluate(board, player);
        }

        List<Move> possibleMoves = MoveGenerator.generateMoves(board);

        // Si pas de coups disponibles
        if (possibleMoves.isEmpty()) {
            return Evaluator.evaluate(board, player);
        }

        if (isMaximizing) {
            int bestScore = Integer.MIN_VALUE;

            for (Move move : possibleMoves) {
                // Joue le coup
                Board newBoard = new Board(board);
                newBoard.makeMove(move.getRow(), move.getCol(), player);

                // Évalue récursivement
                int score = minimax(newBoard, depth - 1, alpha, beta, false, player);

                // Met à jour meilleur score
                bestScore = Math.max(bestScore, score);

                // Met à jour alpha
                alpha = Math.max(alpha, bestScore);

                // Élagage alpha-beta
                if (beta <= alpha) {
                    break;
                }
            }

            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;

            for (Move move : possibleMoves) {
                // Joue le coup
                Board newBoard = new Board(board);
                newBoard.makeMove(move.getRow(), move.getCol(), opponent);

                // Évalue récursivement
                int score = minimax(newBoard, depth - 1, alpha, beta, true, player);

                // Met à jour meilleur score
                bestScore = Math.min(bestScore, score);

                // Met à jour beta
                beta = Math.min(beta, bestScore);

                // Élagage alpha-beta
                if (beta <= alpha) {
                    break;
                }
            }

            return bestScore;
        }
    }

    // Exception pour gérer le timeout
    private static class TimeoutException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}