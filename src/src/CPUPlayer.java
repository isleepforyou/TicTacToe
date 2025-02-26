import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CPUPlayer {
    private Mark cpuMark;
    private Mark opponentMark;
    private SearchEngine searchEngine;
    private Pondering pondering;
    private ExecutorService executorService;

    // Compteur de nœuds explorés
    private AtomicInteger numExploredNodes;
    private int maxDepthReached;

    public CPUPlayer(Mark cpu) {
        this.cpuMark = cpu;
        this.opponentMark = (cpu == Mark.X ? Mark.O : Mark.X);
        this.numExploredNodes = new AtomicInteger(0);

        // Initialiser le pool de threads
        int numThreads = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(numThreads);

        // Créer les composants
        TranspositionTable transpositionTable = new TranspositionTable();
        BoardEvaluator evaluator = new BoardEvaluator(cpuMark, opponentMark);
        MoveOrderer moveOrderer = new MoveOrderer(cpuMark, opponentMark);

        // Initialiser le moteur de recherche
        this.searchEngine = new SearchEngine(
                cpuMark,
                opponentMark,
                transpositionTable,
                evaluator,
                moveOrderer,
                executorService,
                numExploredNodes
        );

        // Initialiser le pondering
        this.pondering = new Pondering(
                cpuMark,
                opponentMark,
                transpositionTable,
                evaluator,
                moveOrderer,
                executorService
        );
    }

    public int getNumOfExploredNodes() {
        return numExploredNodes.get();
    }

    public int getMaxDepthReached() {
        return maxDepthReached;
    }

    public ArrayList<Move> getNextMoveMinMax(Board board) {
        return getNextMoveAB(board);
    }

    public ArrayList<Move> getNextMoveAB(Board board) {
        // Arrêter le pondering si en cours
        pondering.stopPondering();

        // Réinitialiser le compteur
        numExploredNodes.set(0);

        // Vérifier si nous avons des résultats de pondering pour ce plateau
        PonderingResult ponderResult = pondering.getResultForBoard(board);

        // Résultats de recherche
        SearchResult result;

        if (ponderResult != null) {
            System.out.println("Utilisation des résultats de pondering! Profondeur pré-calculée: " +
                    ponderResult.getDepth());

            // Continuer la recherche à partir de la profondeur suivante
            result = searchEngine.searchFromDepth(board, ponderResult.getDepth() + 1, ponderResult);
        } else {
            // Aucun résultat de pondering, démarrer une recherche normale
            result = searchEngine.search(board);
        }

        // Mettre à jour les statistiques
        this.maxDepthReached = result.getMaxDepthReached();

        // Nettoyer les résultats de pondering obsolètes
        pondering.clearResults();

        // Démarrer le pondering pour le prochain tour
        ArrayList<Move> bestMoves = result.getBestMoves();
        if (!bestMoves.isEmpty()) {
            pondering.startPondering(board, bestMoves.get(0));
        }

        return bestMoves;
    }

    public void shutdown() {
        pondering.stopPondering();

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}