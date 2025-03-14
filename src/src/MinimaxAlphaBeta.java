import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Algorithme Minimax avec élagage Alpha-Beta pour Ultimate Tic-Tac-Toe
 * Version améliorée avec multithreading, tables de transposition et tri des coups
 */
public class MinimaxAlphaBeta {
    private static final int MAX_DEPTH = 20;
    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

    private static long timeLimit;
    private static long startTime;

    // Table de transposition pour mémoriser les positions évaluées
    private static TranspositionTable transpositionTable = new TranspositionTable();

    // Hachage Zobrist pour identifier rapidement les positions
    private static ZobristHash zobristHash = new ZobristHash();

    // Pool de threads pour la recherche parallèle
    private static ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

    /**
     * Trouve le meilleur coup pour un joueur dans les limites de temps données
     */
    public static Move findBestMove(Board board, int player, long timeLimitMillis) {
        timeLimit = timeLimitMillis;
        startTime = System.currentTimeMillis();

        // Vide la table de transposition au début d'une nouvelle recherche
        transpositionTable.clear();

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
                Move move = findBestMoveAtDepth(board, player, depth, remainingTime);
                if (move != null) {
                    bestMove = move;
                    maxDepthReached = depth;

                    // Si on trouve un coup gagnant, on peut s'arrêter
                    Board testBoard = new Board(board);
                    testBoard.makeMove(move.getRow(), move.getCol(), player);
                    if (testBoard.checkGameStatus() == player) {
                        break;
                    }
                }
            } catch (Exception e) {
                // En cas d'erreur ou timeout, on utilise le meilleur coup trouvé
                break;
            }
        }

        System.out.println("Move selected at depth: " + maxDepthReached);
        System.out.println("Transposition table size: " + transpositionTable.size());

        // Évalue le score du meilleur coup pour l'afficher
        if (bestMove != null) {
            Board testBoard = new Board(board);
            testBoard.makeMove(bestMove.getRow(), bestMove.getCol(), player);
            int score = Evaluator.evaluate(testBoard, player);
            System.out.println("Move score: " + score);

            // Si aucun coup valide trouvé (peu probable), génère un coup simple
            if (bestMove == null) {
                List<Move> moves = MoveGenerator.generateMoves(board);
                if (!moves.isEmpty()) {
                    bestMove = moves.get(0);
                }
            }

            return bestMove;
        }
        return bestMove;
    }

    /**
     * Trouve le meilleur coup à une profondeur spécifique en utilisant le multithreading
     */
    private static Move findBestMoveAtDepth(Board board, int player, int depth, long remainingTime) throws Exception {
        List<Move> possibleMoves = MoveGenerator.generateMoves(board);

        // Si un seul coup possible, pas besoin de chercher
        if (possibleMoves.size() == 1) {
            return possibleMoves.get(0);
        }

        // Trie les coups pour essayer les plus prometteurs d'abord
        possibleMoves = sortMoves(board, possibleMoves, player);

        // Utilise le multithreading seulement pour les premiers niveaux de l'arbre
        if (depth <= 4 && possibleMoves.size() > 1) {
            return findBestMoveParallel(board, possibleMoves, player, depth, remainingTime);
        } else {
            return findBestMoveSequential(board, possibleMoves, player, depth);
        }
    }

    /**
     * Recherche parallèle des meilleurs coups au premier niveau
     */
    private static Move findBestMoveParallel(Board board, List<Move> possibleMoves, int player, int depth, long remainingTime) throws Exception {
        // Crée des tâches pour l'évaluation parallèle des coups
        List<SearchTask> tasks = new ArrayList<>();
        for (Move move : possibleMoves) {
            SearchTask task = new SearchTask(board, move, player, depth);
            tasks.add(task);
        }

        try {
            // Soumet les tâches pour exécution
            List<Future<MoveScore>> results = executor.invokeAll(tasks,
                    remainingTime - 100, TimeUnit.MILLISECONDS);

            // Trouve le meilleur coup parmi les résultats
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
                        // En cas d'erreur, utilise le coup sans évaluation
                        if (bestMove == null) {
                            bestMove = possibleMoves.get(i);
                        }
                    }
                }
            }

            return bestMove != null ? bestMove : possibleMoves.get(0);

        } catch (InterruptedException e) {
            // Si interrompu, retourne le premier coup comme fallback
            return possibleMoves.get(0);
        }
    }

    /**
     * Recherche séquentielle des meilleurs coups (pour les profondeurs importantes)
     */
    private static Move findBestMoveSequential(Board board, List<Move> possibleMoves, int player, int depth) throws TimeoutException {
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
     * Trie les coups pour évaluer les plus prometteurs en premier
     */
    private static List<Move> sortMoves(Board board, List<Move> moves, int player) {
        // Vérifie si on a un meilleur coup stocké pour cette position
        long boardHash = zobristHash.computeHash(board);
        Move bestMove = transpositionTable.getBestMove(boardHash);

        // Priorise le meilleur coup stocké s'il est disponible
        if (bestMove != null) {
            moves.remove(bestMove);
            moves.add(0, bestMove);
        }

        // On pourrait ajouter d'autres heuristiques pour le tri des coups
        return moves;
    }

    /**
     * Algorithme minimax avec élagage alpha-beta et table de transposition
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

        // Vérifie la table de transposition
        long boardHash = zobristHash.computeHash(board);
        Integer cachedScore = transpositionTable.probe(boardHash, depth, alpha, beta);
        if (cachedScore != null) {
            return cachedScore;
        }

        List<Move> possibleMoves = MoveGenerator.generateMoves(board);

        // Si aucun coup possible, évalue la position actuelle
        if (possibleMoves.isEmpty()) {
            return Evaluator.evaluate(board, player);
        }

        // Trie les coups pour un meilleur élagage
        possibleMoves = sortMoves(board, possibleMoves, isMaximizing ? player : opponent);

        int bestScore;
        int nodeType = TranspositionTable.UPPER_BOUND;
        Move bestMove = null;

        if (isMaximizing) {
            // Tour du joueur (maximisation du score)
            bestScore = Integer.MIN_VALUE;

            for (Move move : possibleMoves) {
                Board newBoard = new Board(board);
                newBoard.makeMove(move.getRow(), move.getCol(), player);

                int score = minimax(newBoard, depth - 1, alpha, beta, false, player);

                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }

                alpha = Math.max(alpha, bestScore);

                // Élagage alpha-beta
                if (beta <= alpha) {
                    break;
                }
            }

            nodeType = bestScore <= alpha ? TranspositionTable.UPPER_BOUND :
                    (bestScore >= beta ? TranspositionTable.LOWER_BOUND : TranspositionTable.EXACT);

        } else {
            // Tour de l'adversaire (minimisation du score)
            bestScore = Integer.MAX_VALUE;

            for (Move move : possibleMoves) {
                Board newBoard = new Board(board);
                newBoard.makeMove(move.getRow(), move.getCol(), opponent);

                int score = minimax(newBoard, depth - 1, alpha, beta, true, player);

                if (score < bestScore) {
                    bestScore = score;
                    bestMove = move;
                }

                beta = Math.min(beta, bestScore);

                // Élagage alpha-beta
                if (beta <= alpha) {
                    break;
                }
            }

            nodeType = bestScore <= alpha ? TranspositionTable.UPPER_BOUND :
                    (bestScore >= beta ? TranspositionTable.LOWER_BOUND : TranspositionTable.EXACT);
        }

        // Stocke le résultat dans la table de transposition
        transpositionTable.store(boardHash, bestScore, depth, nodeType, bestMove);

        return bestScore;
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
     * Nettoyage des ressources
     */
    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Tâche de recherche pour l'exécution parallèle
     */
    private static class SearchTask implements Callable<MoveScore> {
        private Board board;
        private Move move;
        private int player;
        private int depth;

        public SearchTask(Board board, Move move, int player, int depth) {
            this.board = board;
            this.move = move;
            this.player = player;
            this.depth = depth;
        }

        @Override
        public MoveScore call() throws Exception {
            // Crée une copie du plateau et joue le coup
            Board newBoard = new Board(board);
            newBoard.makeMove(move.getRow(), move.getCol(), player);

            // Applique la recherche minimax à partir de cette position
            int score = minimax(newBoard, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, player);

            return new MoveScore(move, score);
        }
    }

    /**
     * Classe pour stocker un coup et son score
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
     * Exception pour gérer le timeout
     */
    private static class TimeoutException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}