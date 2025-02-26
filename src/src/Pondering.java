import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Pondering {
    private Mark cpuMark;
    private Mark opponentMark;
    private TranspositionTable transpositionTable;
    private BoardEvaluator evaluator;
    private MoveOrderer moveOrderer;
    private ExecutorService executorService;

    // Variables de pondering
    private Board ponderingBoard;
    private Future<?> ponderingFuture;
    private AtomicBoolean isPondering;
    private Map<String, PonderingResult> ponderingResults;

    // Profondeur maximale pour le pondering
    private static final int MAX_PONDERING_DEPTH = 7;

    public Pondering(Mark cpuMark, Mark opponentMark,
                     TranspositionTable transpositionTable,
                     BoardEvaluator evaluator,
                     MoveOrderer moveOrderer,
                     ExecutorService executorService) {
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
        this.transpositionTable = transpositionTable;
        this.evaluator = evaluator;
        this.moveOrderer = moveOrderer;
        this.executorService = executorService;

        this.isPondering = new AtomicBoolean(false);
        this.ponderingResults = new ConcurrentHashMap<>();
    }

    // Démarrer le pondering
    public void startPondering(Board originalBoard, Move ourMove) {
        // Créer une copie du plateau avec notre coup joué
        Board afterOurMove = originalBoard.copy();
        afterOurMove.play(ourMove, cpuMark);

        // Trouver le meilleur coup probable de l'adversaire
        ArrayList<Move> opponentMoves = afterOurMove.getAvailableMoves();
        if (opponentMoves.isEmpty()) return;

        // Prendre le coup le plus probable de l'adversaire
        moveOrderer.sortMoves(opponentMoves, afterOurMove, opponentMark);
        Move predictedOpponentMove = opponentMoves.get(0);

        // Plateau après le coup probable de l'adversaire
        ponderingBoard = afterOurMove.copy();
        ponderingBoard.play(predictedOpponentMove, opponentMark);

        // Lancer le pondering
        isPondering.set(true);
        ponderingFuture = executorService.submit(() -> doPondering());
    }

    // Processus de pondering en arrière-plan
    private void doPondering() {
        try {
            ArrayList<Move> ponderBestMoves = new ArrayList<>();
            int ponderBestScore = -1000; // -INF
            int ponderMaxDepth = 0;

            System.out.println("Démarrage du pondering...");

            // Recherche pendant le temps de l'adversaire
            for (int depth = 3; depth <= MAX_PONDERING_DEPTH; depth++) {
                if (!isPondering.get()) break;

                ArrayList<Move> currentMoves = new ArrayList<>();
                int currentScore = -1000;

                ArrayList<Move> moves = ponderingBoard.getAvailableMoves();
                if (moves.isEmpty()) break;

                moveOrderer.sortMoves(moves, ponderingBoard, cpuMark);

                // Recherche simplifiée
                for (Move move : moves) {
                    if (!isPondering.get()) break;

                    Board newBoard = ponderingBoard.copy();
                    newBoard.play(move, cpuMark);

                    // Utiliser une profondeur réduite pour le pondering
                    int score = alphaBeta(newBoard, false, -1000, 1000, depth - 1);

                    if (score > currentScore) {
                        currentScore = score;
                        currentMoves.clear();
                        currentMoves.add(move);
                    } else if (score == currentScore) {
                        currentMoves.add(move);
                    }
                }

                if (isPondering.get()) {
                    ponderBestMoves = new ArrayList<>(currentMoves);
                    ponderBestScore = currentScore;
                    ponderMaxDepth = depth;

                    // Sauvegarder les résultats
                    String boardKey = boardToString(ponderingBoard);
                    ponderingResults.put(boardKey, new PonderingResult(
                            ponderBestMoves, ponderBestScore, ponderMaxDepth
                    ));

                    System.out.println("Pondering: profondeur " + depth + " complétée");
                }
            }

            System.out.println("Pondering terminé. Profondeur maximale: " + ponderMaxDepth);
        } catch (Exception e) {
            System.err.println("Erreur dans le pondering: " + e.getMessage());
        }
    }

    // AlphaBeta simplifié pour le pondering
    private int alphaBeta(Board board, boolean isMaximizing, int alpha, int beta, int depth) {
        if (!isPondering.get() || depth <= 0 || board.isTerminal()) {
            return evaluator.evaluate(board);
        }

        ArrayList<Move> moves = board.getAvailableMoves();
        moveOrderer.sortMoves(moves, board, isMaximizing ? cpuMark : opponentMark);

        if (isMaximizing) {
            int best = -1000;
            for (Move move : moves) {
                if (!isPondering.get()) break;

                Board newBoard = board.copy();
                newBoard.play(move, cpuMark);
                int value = alphaBeta(newBoard, false, alpha, beta, depth - 1);
                best = Math.max(best, value);
                alpha = Math.max(alpha, best);
                if (beta <= alpha) break;
            }
            return best;
        } else {
            int best = 1000;
            for (Move move : moves) {
                if (!isPondering.get()) break;

                Board newBoard = board.copy();
                newBoard.play(move, opponentMark);
                int value = alphaBeta(newBoard, true, alpha, beta, depth - 1);
                best = Math.min(best, value);
                beta = Math.min(beta, best);
                if (beta <= alpha) break;
            }
            return best;
        }
    }

    // Arrêter le pondering
    public void stopPondering() {
        if (isPondering.get()) {
            isPondering.set(false);

            if (ponderingFuture != null) {
                ponderingFuture.cancel(false);

                try {
                    ponderingFuture.get(100, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    // Ignorer les exceptions d'annulation
                }

                ponderingFuture = null;
            }

            System.out.println("Pondering arrêté");
        }
    }

    // Vérifier si nous avons des résultats pour ce plateau
    public PonderingResult getResultForBoard(Board board) {
        String key = boardToString(board);
        return ponderingResults.get(key);
    }

    // Convertir un plateau en chaîne pour l'utiliser comme clé
    private String boardToString(Board board) {
        StringBuilder sb = new StringBuilder();
        Mark[][] grid = board.getGrid();

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (grid[r][c] == Mark.X) sb.append('X');
                else if (grid[r][c] == Mark.O) sb.append('O');
                else sb.append('.');
            }
        }

        // Ajouter l'information sur le prochain plateau local
        sb.append("|").append(board.getNextLocalRow()).append(",").append(board.getNextLocalCol());

        return sb.toString();
    }

    // Nettoyer les résultats de pondering obsolètes
    public void clearResults() {
        ponderingResults.clear();
    }
}