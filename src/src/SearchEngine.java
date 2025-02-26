import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchEngine {
    private static final int INF = 1000;
    private static final int INITIAL_DEPTH = 3;     // Commencer à une profondeur plus faible
    private static final int MAX_DEPTH = 20;        // Profondeur maximale théorique plus élevée
    private static final long MAX_SEARCH_TIME = 2950; // 2.95 secondes (pour laisser une marge)

    private Mark cpuMark;
    private Mark opponentMark;
    private TranspositionTable transpositionTable;
    private BoardEvaluator evaluator;
    private MoveOrderer moveOrderer;
    private ExecutorService executorService;
    private AtomicInteger numExploredNodes;

    // Variables de recherche
    private long startTime;
    private AtomicBoolean timeUp;

    public SearchEngine(Mark cpuMark, Mark opponentMark,
                        TranspositionTable transpositionTable,
                        BoardEvaluator evaluator,
                        MoveOrderer moveOrderer,
                        ExecutorService executorService,
                        AtomicInteger numExploredNodes) {
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
        this.transpositionTable = transpositionTable;
        this.evaluator = evaluator;
        this.moveOrderer = moveOrderer;
        this.executorService = executorService;
        this.numExploredNodes = numExploredNodes;
    }

    // Recherche complète
    public SearchResult search(Board board) {
        return searchFromDepth(board, INITIAL_DEPTH, null);
    }

    // Recherche à partir d'une profondeur spécifique
    public SearchResult searchFromDepth(Board board, int startDepth, Object unusedParam) {
        // Initialisation
        startTime = System.currentTimeMillis();
        timeUp = new AtomicBoolean(false);

        ArrayList<Move> bestMoves = new ArrayList<>();
        int bestScore = -INF;
        int maxDepthReached = 0;

        // Recherche itérative
        for (int depth = startDepth; depth <= MAX_DEPTH; depth++) {
            // Vérifier si nous avons encore du temps
            if (timeUp.get()) break;

            SearchResult depthResult = searchAtDepth(board, depth);

            // Si recherche complétée avec succès, mettre à jour les résultats
            if (!timeUp.get()) {
                bestMoves = depthResult.getBestMoves();
                bestScore = depthResult.getBestScore();
                maxDepthReached = depth;

                // Si on a trouvé un coup gagnant, arrêter la recherche
                if (bestScore >= 90) {
                    System.out.println("Coup gagnant trouvé à la profondeur " + depth);
                    break;
                }

                // Estimer si nous avons le temps pour une itération supplémentaire
                long elapsedTime = System.currentTimeMillis() - startTime;
                long remainingTime = MAX_SEARCH_TIME - elapsedTime;

                // Estimer le temps nécessaire pour la prochaine itération
                long estimatedNextTime;
                if (depth <= 5) {
                    estimatedNextTime = (long)(elapsedTime * 3.0);
                } else if (depth <= 8) {
                    estimatedNextTime = (long)(elapsedTime * 2.5);
                } else {
                    estimatedNextTime = (long)(elapsedTime * 2.0);
                }

                System.out.println("Profondeur " + depth + " terminée en " + elapsedTime +
                        " ms, estimation pour profondeur suivante: " + estimatedNextTime +
                        " ms, temps restant: " + remainingTime + " ms");

                if (estimatedNextTime > remainingTime) {
                    System.out.println("Arrêt de la recherche - temps insuffisant pour la profondeur suivante");
                    break;
                }
            } else {
                System.out.println("Temps écoulé pendant la recherche à la profondeur " + depth);
            }
        }

        // Afficher des statistiques
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Profondeur: " + maxDepthReached +
                ", Nœuds: " + numExploredNodes.get() +
                ", Temps: " + totalTime + " ms, Score: " + bestScore);

        return new SearchResult(bestMoves, bestScore, maxDepthReached);
    }

    // Recherche à une profondeur fixe
    private SearchResult searchAtDepth(Board board, int depth) {
        ArrayList<Move> currentDepthMoves = new ArrayList<>();
        int currentDepthScore = -INF;

        // Obtenir les coups disponibles
        ArrayList<Move> moves = board.getAvailableMoves();

        // Trier les coups
        moveOrderer.sortMoves(moves, board, cpuMark);

        // Limiter le nombre de coups à considérer pour les grandes profondeurs
        if (depth > 8 && moves.size() > 10) {
            moves = new ArrayList<>(moves.subList(0, 10));
        } else if (depth > 6 && moves.size() > 15) {
            moves = new ArrayList<>(moves.subList(0, 15));
        }

        // Recherche parallèle
        try {
            currentDepthScore = searchMovesParallel(board, moves, depth, currentDepthMoves);
        } catch (Exception e) {
            System.err.println("Erreur dans la recherche: " + e.getMessage());
            timeUp.set(true);
        }

        return new SearchResult(currentDepthMoves, currentDepthScore, depth);
    }

    // Recherche parallèle
    private int searchMovesParallel(Board board, ArrayList<Move> moves, int depth, ArrayList<Move> resultMoves)
            throws InterruptedException, ExecutionException {

        int bestValue = -INF;
        List<Future<MoveScore>> futures = new ArrayList<>();

        // Calculer le timeout pour cette profondeur
        long elapsedTime = System.currentTimeMillis() - startTime;
        long timeoutMs = MAX_SEARCH_TIME - elapsedTime;

        // Lancer les recherches en parallèle
        for (Move move : moves) {
            futures.add(executorService.submit(() -> {
                if (isTimeUp()) {
                    return new MoveScore(move, -INF);
                }

                Board newBoard = board.copy();
                newBoard.play(move, cpuMark);
                int score = alphaBeta(newBoard, false, -INF, INF, depth - 1, 1);

                if (isTimeUp()) {
                    return new MoveScore(move, -INF);
                }

                return new MoveScore(move, score);
            }));
        }

        // Collecter les résultats
        List<MoveScore> scores = new ArrayList<>();
        for (Future<MoveScore> future : futures) {
            try {
                MoveScore result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                scores.add(result);
            } catch (TimeoutException e) {
                timeUp.set(true);
                System.out.println("Timeout lors de la collecte des résultats");
                break;
            }
        }

        // Trouver le meilleur score
        resultMoves.clear();
        for (MoveScore ms : scores) {
            if (ms.score > bestValue) {
                bestValue = ms.score;
                resultMoves.clear();
                resultMoves.add(ms.move);
            } else if (ms.score == bestValue) {
                resultMoves.add(ms.move);
            }
        }

        return bestValue;
    }

    // Méthode pour vérifier si le temps est écoulé
    private boolean isTimeUp() {
        if (timeUp.get()) return true;

        if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME) {
            timeUp.set(true);
            return true;
        }

        return false;
    }

    // Algorithme Alpha-Beta principal
    private int alphaBeta(Board board, boolean isMaximizing, int alpha, int beta, int depth, int ply) {
        // Vérifier régulièrement le temps
        if (ply % 3 == 0 && isTimeUp()) {
            return 0;
        }

        numExploredNodes.incrementAndGet();

        // Consulter la table de transposition
        long hash = transpositionTable.hash(board, isMaximizing);
        TranspositionEntry entry = transpositionTable.get(hash);
        if (entry != null && entry.getDepth() >= depth) {
            return entry.getScore();
        }

        // Vérifier les cas terminaux
        if (board.isTerminal()) {
            int score = board.evaluate(cpuMark);
            transpositionTable.put(hash, score, depth);
            return score;
        }

        // Si profondeur atteinte, évaluer la position
        if (depth <= 0) {
            int score = evaluator.evaluate(board);
            transpositionTable.put(hash, score, 0);
            return score;
        }

        ArrayList<Move> moves = board.getAvailableMoves();

        // Trier les coups
        if (depth > 2) {
            moveOrderer.sortMoves(moves, board, isMaximizing ? cpuMark : opponentMark);
        }

        // Limiter le nombre de coups à explorer pour les nœuds moins importants
        if (depth <= 3 && moves.size() > 12 && ply > 2) {
            moves = new ArrayList<>(moves.subList(0, 12));
        }

        if (isMaximizing) {
            int best = -INF;
            for (Move move : moves) {
                if (ply % 3 == 0 && isTimeUp()) break;

                Board newBoard = board.copy();
                newBoard.play(move, cpuMark);
                int value = alphaBeta(newBoard, false, alpha, beta, depth - 1, ply + 1);
                best = Math.max(best, value);
                alpha = Math.max(alpha, best);
                if (beta <= alpha) break;
            }
            transpositionTable.put(hash, best, depth);
            return best;
        } else {
            int best = INF;
            for (Move move : moves) {
                if (ply % 3 == 0 && isTimeUp()) break;

                Board newBoard = board.copy();
                newBoard.play(move, opponentMark);
                int value = alphaBeta(newBoard, true, alpha, beta, depth - 1, ply + 1);
                best = Math.min(best, value);
                beta = Math.min(beta, best);
                if (beta <= alpha) break;
            }
            transpositionTable.put(hash, best, depth);
            return best;
        }
    }

    // Classe interne pour les scores de coups
    private class MoveScore {
        Move move;
        int score;

        public MoveScore(Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }
}