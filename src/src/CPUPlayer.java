import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CPUPlayer {
    private Mark cpuMark;
    private Mark opponentMark;
    private SearchEngine searchEngine;
    private ExecutorService executorService;

    // Compteur de nœuds explorés
    private AtomicInteger numExploredNodes;
    private int maxDepthReached;

    // Constantes de temps
    private static final long MAX_MOVE_TIME = 2950; // 2.95 secondes max pour un coup

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
        // Réinitialiser le compteur
        numExploredNodes.set(0);

        long startTime = System.currentTimeMillis();

        // Lancer la recherche
        SearchResult result = searchEngine.search(board);

        // Mettre à jour les statistiques
        this.maxDepthReached = result.getMaxDepthReached();

        long endTime = System.currentTimeMillis();
        System.out.println("Temps de calcul total: " + (endTime - startTime) + " ms");

        return result.getBestMoves();
    }

    // Pour compatibilité avec le client existant, mais ne fait rien maintenant
    public void stopPondering() {
        // Ne fait rien - le pondering a été supprimé
    }

    public void shutdown() {
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