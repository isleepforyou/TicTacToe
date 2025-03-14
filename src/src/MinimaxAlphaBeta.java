import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Algorithme Minimax avec élagage Alpha-Beta pour Ultimate Tic-Tac-Toe
 * Implémentation avec multi-threading
 */
public class MinimaxAlphaBeta {
    private static final int MAX_DEPTH = 30;
    private static long timeLimit;
    private static long startTime;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private static AtomicInteger nodesExplored = new AtomicInteger(0);
    private static volatile boolean timeoutOccurred = false;

    /**
     * Trouve le meilleur coup pour un joueur dans les limites de temps données
     */
    public static Move findBestMove(Board board, int player, long timeLimitMillis) {
        timeLimit = timeLimitMillis;
        startTime = System.currentTimeMillis();
        Move bestMove = null;

        int maxDepthReached = 0;
        timeoutOccurred = false;

        System.out.println("Using " + NUM_THREADS + " threads for search");

        // Approfondissement itératif - commence à profondeur 1 et augmente progressivement
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long remainingTime = timeLimit - elapsedTime;

            // Arrête si moins de 10% du temps reste
            if (remainingTime < (timeLimit * 0.1)) {
                break;
            }

            try {
                nodesExplored.set(0);

                // Pour les profondeurs 1 et 2, utiliser l'algorithme séquentiel
                // pour établir une bonne valeur de base
                Move move;
                if (depth <= 2) {
                    move = findBestMoveSequential(board, player, depth);
                } else {
                    // Utiliser l'algorithme parallèle pour les profondeurs supérieures
                    move = findBestMoveParallel(board, player, depth, remainingTime);
                }

                if (move != null && !timeoutOccurred) {
                    bestMove = move;
                    maxDepthReached = depth;
                    System.out.println("Depth " + depth + " completed. Nodes explored: " + nodesExplored.get());
                }
            } catch (TimeoutException e) {
                System.out.println("Timeout at depth " + depth + ". Nodes explored: " + nodesExplored.get());
                break;
            } catch (Exception e) {
                System.out.println("Error at depth " + depth + ": " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }

        System.out.println("Move selected at depth: " + maxDepthReached);
        return bestMove;
    }

    /**
     * Version séquentielle originale pour les faibles profondeurs
     */
    private static Move findBestMoveSequential(Board board, int player, int depth) throws TimeoutException {
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
     * Trouve le meilleur coup à une profondeur spécifique en parallèle
     */
    private static Move findBestMoveParallel(Board board, int player, int depth, long remainingTime)
            throws TimeoutException, InterruptedException {
        List<Move> possibleMoves = MoveGenerator.generateMoves(board);

        if (possibleMoves.isEmpty()) {
            return null;
        }

        // Si peu de coups sont disponibles, pas besoin de paralléliser
        if (possibleMoves.size() == 1) {
            return possibleMoves.get(0);
        }

        // Créer un thread pool
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(NUM_THREADS, possibleMoves.size()));

        List<MoveEvaluationTask> tasks = new ArrayList<>();

        // Créer une tâche pour chaque coup possible
        for (Move move : possibleMoves) {
            MoveEvaluationTask task = new MoveEvaluationTask(board, move, player, depth);
            tasks.add(task);
        }

        try {
            // Exécuter toutes les tâches et attendre le résultat
            List<Future<MoveScore>> results = executor.invokeAll(tasks,
                    remainingTime * 90 / 100, // 90% du temps restant
                    TimeUnit.MILLISECONDS);

            // Trouver le meilleur score
            int bestScore = Integer.MIN_VALUE;
            Move bestMove = null;

            for (int i = 0; i < results.size(); i++) {
                Future<MoveScore> future = results.get(i);
                if (!future.isCancelled()) {
                    try {
                        MoveScore result = future.get();
                        if (result.score > bestScore) {
                            bestScore = result.score;
                            bestMove = result.move;
                        }
                    } catch (Exception e) {
                        // Ignorer les erreurs individuelles des tâches
                    }
                }
            }

            return bestMove;

        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Classe pour représenter un coup et son score
     */
    private static class MoveScore {
        Move move;
        int score;

        MoveScore(Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }

    /**
     * Tâche pour évaluer un coup spécifique
     */
    private static class MoveEvaluationTask implements Callable<MoveScore> {
        private final Board board;
        private final Move move;
        private final int player;
        private final int depth;

        MoveEvaluationTask(Board board, Move move, int player, int depth) {
            this.board = new Board(board); // Copie du plateau pour éviter les problèmes de concurrence
            this.move = move;
            this.player = player;
            this.depth = depth;
        }

        @Override
        public MoveScore call() throws Exception {
            try {
                // Joue le coup sur la copie du plateau
                board.makeMove(move.getRow(), move.getCol(), player);

                // Évalue le coup avec minimax
                int score = minimax(board, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, player);

                return new MoveScore(move, score);
            } catch (TimeoutException e) {
                timeoutOccurred = true;
                throw e;
            }
        }
    }

    /**
     * Algorithme minimax avec élagage alpha-beta
     * Version inchangée de l'original pour préserver le comportement
     */
    private static int minimax(Board board, int depth, int alpha, int beta, boolean isMaximizing, int player)
            throws TimeoutException {
        nodesExplored.incrementAndGet();

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
            timeoutOccurred = true;
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