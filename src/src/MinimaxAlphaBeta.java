import java.util.List;

/**
 * Algorithme Minimax avec élagage Alpha-Beta pour Ultimate Tic-Tac-Toe
 */
public class MinimaxAlphaBeta {
    private static final int MAX_DEPTH = 20;
    private static long timeLimit;
    private static long startTime;

    /**
     * Trouve le meilleur coup pour un joueur dans les limites de temps données
     */
    public static Move findBestMove(Board board, int player, long timeLimitMillis) {
        timeLimit = timeLimitMillis;
        startTime = System.currentTimeMillis();
        Move bestMove = null;

        int maxDepthReached = 0;

        // Approfondissement itératif - commence à profondeur 1 et augmente progressivement
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long remainingTime = timeLimit - elapsedTime;

            // Arrête si moins de 10% du temps reste
            if (remainingTime < (timeLimit * 0.1)) {
                break;
            }

            try {
                Move move = findBestMoveAtDepth(board, player, depth);
                bestMove = move;
                maxDepthReached = depth;
            } catch (TimeoutException e) {
                break;
            }
        }

        System.out.println("Move selected at depth: " + maxDepthReached);
        return bestMove;
    }

    /**
     * Trouve le meilleur coup à une profondeur spécifique
     */
    private static Move findBestMoveAtDepth(Board board, int player, int depth) throws TimeoutException {
        List<Move> possibleMoves = MoveGenerator.generateMoves(board);
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Move move : possibleMoves) {

            checkTimeLimit();

            // Crée une copie du plateau et joue le coup
            Board newBoard = new Board(board);
            newBoard.makeMove(move.getRow(), move.getCol(), player);

            // Évalue le coup avec minimax
            int score = minimax(newBoard, depth - 1, alpha, beta, false, player);

            // Met à jour le meilleur coup si nécessaire
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            // Met à jour alpha pour l'élagage
            alpha = Math.max(alpha, bestScore);
        }

        return bestMove;
    }

    /**
     * Algorithme minimax avec élagage alpha-beta
     */
    private static int minimax(Board board, int depth, int alpha, int beta, boolean isMaximizing, int player) throws TimeoutException {

        if (depth % 3 == 0) {
            checkTimeLimit();
        }

        int opponent = (player == 4) ? 2 : 4;
        int gameStatus = board.checkGameStatus();

        // Conditions de terminaison: jeu terminé ou profondeur maximale atteinte
        if (gameStatus != 0 || depth == 0) {
            return Evaluator.evaluate(board, player);
        }

        List<Move> possibleMoves = MoveGenerator.generateMoves(board);

        // Si aucun coup possible, évalue la position actuelle
        if (possibleMoves.isEmpty()) {
            return Evaluator.evaluate(board, player);
        }

        if (isMaximizing) {
            // Tour du joueur (maximisation du score)
            int bestScore = Integer.MIN_VALUE;

            for (Move move : possibleMoves) {
                Board newBoard = new Board(board);
                newBoard.makeMove(move.getRow(), move.getCol(), player);

                int score = minimax(newBoard, depth - 1, alpha, beta, false, player);
                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, bestScore);

                // Élagage alpha-beta
                if (beta <= alpha) {
                    break;
                }
            }

            return bestScore;
        } else {
            // Tour de l'adversaire (minimisation du score)
            int bestScore = Integer.MAX_VALUE;

            for (Move move : possibleMoves) {
                Board newBoard = new Board(board);
                newBoard.makeMove(move.getRow(), move.getCol(), opponent);

                int score = minimax(newBoard, depth - 1, alpha, beta, true, player);
                bestScore = Math.min(bestScore, score);
                beta = Math.min(beta, bestScore);

                // Élagage alpha-beta
                if (beta <= alpha) {
                    break;
                }
            }

            return bestScore;
        }
    }

    /**
     * Vérifie si la limite de temps est atteinte
     */
    private static void checkTimeLimit() throws TimeoutException {
        if (System.currentTimeMillis() - startTime > timeLimit * 0.95) {
            throw new TimeoutException();
        }
    }

    /**
     * Exception pour gérer le timeout
     */
    private static class TimeoutException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}