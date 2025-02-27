import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchEngine {
    // Paramètres ajustés pour le tic-tac-toe géant
    private static final int INF = 100000;
    private static final int INITIAL_DEPTH = 4;     // Commencer à une profondeur plus faible
    private static final int MAX_DEPTH = 15;        // Profondeur maximale augmentée
    private static final long TIME_MARGIN = 200;    // Marge de sécurité en ms
    private static final long MAX_SEARCH_TIME = 2800; // Temps maximal d'environ 2,8 secondes

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

    // Variables de statistiques
    private int cutoffCount;
    private int bestMoveChanges;

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
    public SearchResult searchFromDepth(Board board, int startDepth, PonderingResult ponderResult) {
        // Initialisation
        startTime = System.currentTimeMillis();
        timeUp = new AtomicBoolean(false);
        cutoffCount = 0;
        bestMoveChanges = 0;

        ArrayList<Move> bestMoves = new ArrayList<>();
        int bestScore = -INF;
        int maxDepthReached = 0;

        // Utiliser les résultats du pondering si disponibles
        if (ponderResult != null) {
            bestMoves = new ArrayList<>(ponderResult.getBestMoves());
            bestScore = ponderResult.getBestScore();
            maxDepthReached = ponderResult.getDepth();
            System.out.println("Résultats de pondering utilisés: " + bestScore + " à la profondeur " + maxDepthReached);
        }

        // Obtenir les coups disponibles initiaux
        ArrayList<Move> moves = board.getAvailableMoves();
        if (moves.isEmpty()) {
            return new SearchResult(new ArrayList<>(), 0, 0);
        }

        // Augmenter progressivement la profondeur (recherche itérative)
        for (int depth = startDepth; depth <= MAX_DEPTH; depth++) {
            // Réinitialiser le compteur pour cette profondeur
            numExploredNodes.set(0);

            ArrayList<Move> currentDepthMoves = new ArrayList<>();
            int currentDepthScore = -INF;

            try {
                // Commencer la recherche pour cette profondeur
                System.out.println("Recherche à la profondeur: " + depth);

                // Recherche parallèle à la racine seulement pour les premiers niveaux
                if (depth <= 7) {
                    currentDepthScore = searchMovesParallel(board, moves, depth, currentDepthMoves);
                } else {
                    // Pour les profondeurs élevées, effectuer une recherche séquentielle
                    // avec les coups déjà triés par la dernière itération
                    moveOrderer.sortMoves(moves, board, cpuMark);
                    currentDepthScore = searchMovesSequential(board, moves, depth, currentDepthMoves);
                }

                // Si on a fini l'itération sans timeout, mettre à jour les meilleurs coups
                if (!timeUp.get()) {
                    bestMoves = new ArrayList<>(currentDepthMoves);
                    bestScore = currentDepthScore;
                    maxDepthReached = depth;
                    bestMoveChanges++;

                    System.out.println("Profondeur " + depth + " complétée. Score: " + bestScore +
                            ", Noeuds: " + numExploredNodes.get() +
                            ", Temps: " + (System.currentTimeMillis() - startTime) + " ms");

                    // Si on a trouvé un coup gagnant, arrêter la recherche
                    if (bestScore >= 1000) {
                        System.out.println("Coup gagnant trouvé!");
                        break;
                    }

                    // Si on a consommé presque tout le temps, arrêter
                    if (System.currentTimeMillis() - startTime >= MAX_SEARCH_TIME - TIME_MARGIN) {
                        System.out.println("Temps presque écoulé, arrêt de la recherche.");
                        break;
                    }
                } else {
                    System.out.println("Timeout à la profondeur " + depth + ", utilisant résultats précédents.");
                    break;
                }
            } catch (Exception e) {
                System.err.println("Erreur pendant la recherche à la profondeur " + depth + ": " + e.getMessage());
                // En cas d'erreur, utiliser les résultats de la dernière profondeur complète
                break;
            }
        }

        // Afficher des statistiques
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Recherche terminée. Profondeur: " + maxDepthReached +
                ", Nœuds explorés: " + numExploredNodes.get() +
                ", Temps total: " + totalTime + " ms" +
                ", Score final: " + bestScore +
                ", Changements de meilleur coup: " + bestMoveChanges);

        // Si aucun coup valide n'a été trouvé, prendre le premier coup disponible
        if (bestMoves.isEmpty() && !moves.isEmpty()) {
            System.out.println("ATTENTION: Aucun meilleur coup trouvé, choix par défaut.");
            bestMoves.add(moves.get(0));
        }

        // Maximiser l'utilisation du temps si nous n'avons pas exploré suffisamment
        long remainingTime = MAX_SEARCH_TIME - (System.currentTimeMillis() - startTime);
        if (remainingTime > 500 && numExploredNodes.get() < 10000) {
            System.out.println("Temps restant: " + remainingTime + "ms. Exploration supplémentaire...");
            try {
                // Exploration supplémentaire pour utiliser le temps restant
                Thread.sleep(Math.min(remainingTime - 50, 500));
            } catch (InterruptedException e) {
                // Ignorer
            }
        }

        return new SearchResult(bestMoves, bestScore, maxDepthReached);
    }

    // Recherche séquentielle des meilleurs coups
    private int searchMovesSequential(Board board, ArrayList<Move> moves, int depth, ArrayList<Move> resultMoves) {
        int bestValue = -INF;
        resultMoves.clear();

        // Trier les coups pour améliorer l'élagage alpha-beta
        moveOrderer.sortMoves(moves, board, cpuMark);

        for (Move move : moves) {
            if (timeUp.get() || System.currentTimeMillis() - startTime > MAX_SEARCH_TIME - TIME_MARGIN) {
                timeUp.set(true);
                return bestValue;
            }

            Board newBoard = board.copy();
            newBoard.play(move, cpuMark);

            int score = alphaBeta(newBoard, false, -INF, INF, depth - 1, 1);

            System.out.println("  Évaluation du coup " + move + ": " + score);

            if (score > bestValue) {
                bestValue = score;
                resultMoves.clear();
                resultMoves.add(move);
            } else if (score == bestValue) {
                resultMoves.add(move);
            }
        }

        return bestValue;
    }

    // Recherche parallèle pour le premier niveau
    private int searchMovesParallel(Board board, ArrayList<Move> moves, int depth, ArrayList<Move> resultMoves)
            throws InterruptedException, ExecutionException {

        int bestValue = -INF;
        List<Future<MoveScore>> futures = new ArrayList<>();
        resultMoves.clear();

        // Trier les coups pour améliorer l'élagage alpha-beta
        moveOrderer.sortMoves(moves, board, cpuMark);

        // Lancer les recherches en parallèle
        for (Move move : moves) {
            futures.add(executorService.submit(() -> {
                if (timeUp.get()) {
                    return new MoveScore(move, -INF);
                }

                Board newBoard = board.copy();
                newBoard.play(move, cpuMark);

                int score = alphaBeta(newBoard, false, -INF, INF, depth - 1, 1);

                if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME - TIME_MARGIN) {
                    timeUp.set(true);
                }

                return new MoveScore(move, score);
            }));
        }

        // Collecter les résultats
        List<MoveScore> scores = new ArrayList<>();
        for (Future<MoveScore> future : futures) {
            try {
                long timeRemaining = MAX_SEARCH_TIME - (System.currentTimeMillis() - startTime);
                if (timeRemaining <= 0) {
                    timeUp.set(true);
                    break;
                }

                MoveScore result = future.get(timeRemaining, TimeUnit.MILLISECONDS);
                scores.add(result);
                System.out.println("  Évaluation parallèle du coup " + result.move + ": " + result.score);
            } catch (TimeoutException e) {
                timeUp.set(true);
                break;
            }
        }

        // Trouver le meilleur score
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

    // Algorithme Alpha-Beta principal
    private int alphaBeta(Board board, boolean isMaximizing, int alpha, int beta, int depth, int ply) {
        // Vérifier le temps
        if (timeUp.get() || System.currentTimeMillis() - startTime > MAX_SEARCH_TIME - TIME_MARGIN) {
            timeUp.set(true);
            return 0;
        }

        numExploredNodes.incrementAndGet();

        // Vérifier les cas terminaux
        if (board.isTerminal()) {
            Mark winner = checkWinner(board);
            if (winner == cpuMark) {
                return 10000 - ply; // Préférer les victoires rapides
            } else if (winner == opponentMark) {
                return -10000 + ply; // Retarder les défaites
            } else {
                return 0; // Match nul
            }
        }

        // Si profondeur atteinte, évaluer la position
        if (depth <= 0) {
            return evaluator.evaluate(board);
        }

        // Consulter la table de transposition
        long hash = BoardHasher.hash(board, isMaximizing);
        TranspositionEntry entry = transpositionTable.get(hash);
        if (entry != null && entry.getDepth() >= depth) {
            return entry.getScore();
        }

        ArrayList<Move> moves = board.getAvailableMoves();

        // Trier les coups pour une meilleure efficacité d'élagage
        moveOrderer.sortMoves(moves, board, isMaximizing ? cpuMark : opponentMark);

        int bestScore;

        if (isMaximizing) {
            bestScore = -INF;
            for (Move move : moves) {
                if (timeUp.get()) break;

                Board newBoard = board.copy();
                newBoard.play(move, cpuMark);

                int score = alphaBeta(newBoard, false, alpha, beta, depth - 1, ply + 1);

                if (score > bestScore) {
                    bestScore = score;
                }

                alpha = Math.max(alpha, bestScore);

                if (beta <= alpha) {
                    cutoffCount++;
                    moveOrderer.addKillerMove(move);
                    break;
                }
            }
        } else {
            bestScore = INF;
            for (Move move : moves) {
                if (timeUp.get()) break;

                Board newBoard = board.copy();
                newBoard.play(move, opponentMark);

                int score = alphaBeta(newBoard, true, alpha, beta, depth - 1, ply + 1);

                if (score < bestScore) {
                    bestScore = score;
                }

                beta = Math.min(beta, bestScore);

                if (beta <= alpha) {
                    cutoffCount++;
                    moveOrderer.addKillerMove(move);
                    break;
                }
            }
        }

        // Stocker dans la table de transposition
        if (!timeUp.get()) {
            transpositionTable.put(hash, bestScore, depth);
        }

        return bestScore;
    }

    // Vérifier s'il y a un gagnant
    private Mark checkWinner(Board board) {
        Mark[][] localBoards = board.getLocalBoards();

        // Vérifier les lignes
        for (int r = 0; r < 3; r++) {
            if (localBoards[r][0] != Mark.EMPTY &&
                    localBoards[r][0] == localBoards[r][1] &&
                    localBoards[r][1] == localBoards[r][2]) {
                return localBoards[r][0];
            }
        }

        // Vérifier les colonnes
        for (int c = 0; c < 3; c++) {
            if (localBoards[0][c] != Mark.EMPTY &&
                    localBoards[0][c] == localBoards[1][c] &&
                    localBoards[1][c] == localBoards[2][c]) {
                return localBoards[0][c];
            }
        }

        // Vérifier les diagonales
        if (localBoards[0][0] != Mark.EMPTY &&
                localBoards[0][0] == localBoards[1][1] &&
                localBoards[1][1] == localBoards[2][2]) {
            return localBoards[0][0];
        }

        if (localBoards[0][2] != Mark.EMPTY &&
                localBoards[0][2] == localBoards[1][1] &&
                localBoards[1][1] == localBoards[2][0]) {
            return localBoards[0][2];
        }

        return Mark.EMPTY;
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