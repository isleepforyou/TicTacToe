import java.util.HashMap;
import java.util.Map;

/**
 * Transposition Table for storing evaluated positions
 * This uses a simple hash map to avoid re-computing positions that have been seen before
 */
public class TranspositionTable {
    // Maximum size of the hash table
    private static final int MAX_TABLE_SIZE = 100_000;

    // Different types of nodes stored in the table
    public static final int EXACT = 0;
    public static final int UPPER_BOUND = 1;
    public static final int LOWER_BOUND = 2;

    // Entry class to store information about each position
    private static class TableEntry {
        int score;      // Evaluation score
        int depth;      // Depth of the search
        int type;       // Type of node (exact, lower bound, upper bound)
        Move bestMove;  // Best move found for this position

        public TableEntry(int score, int depth, int type, Move bestMove) {
            this.score = score;
            this.depth = depth;
            this.type = type;
            this.bestMove = bestMove;
        }
    }

    // Stores the position hashes mapped to their entries
    private Map<Long, TableEntry> table;

    public TranspositionTable() {
        table = new HashMap<>();
    }

    /**
     * Stores an entry in the transposition table
     *
     * @param boardHash The hash of the board position
     * @param score The evaluation score
     * @param depth The depth at which the position was evaluated
     * @param type The type of node (exact, upper bound, lower bound)
     * @param bestMove The best move found for this position
     */
    public void store(long boardHash, int score, int depth, int type, Move bestMove) {
        // Ensure the table doesn't grow too large
        if (table.size() >= MAX_TABLE_SIZE) {
            // Simple strategy: clear the entire table when it gets too large
            // A more sophisticated approach would be to use a replacement strategy
            table.clear();
        }

        table.put(boardHash, new TableEntry(score, depth, type, bestMove));
    }

    /**
     * Retrieves an entry from the transposition table
     *
     * @param boardHash The hash of the board position
     * @param depth The current search depth
     * @param alpha Current alpha value
     * @param beta Current beta value
     * @return The score if found and valid, null otherwise
     */
    public Integer probe(long boardHash, int depth, int alpha, int beta) {
        TableEntry entry = table.get(boardHash);

        if (entry == null || entry.depth < depth) {
            return null;
        }

        // Use the stored value based on its type
        if (entry.type == EXACT) {
            return entry.score;
        } else if (entry.type == LOWER_BOUND && entry.score <= alpha) {
            return alpha;
        } else if (entry.type == UPPER_BOUND && entry.score >= beta) {
            return beta;
        }

        return null;
    }

    /**
     * Gets the best move stored for a position
     *
     * @param boardHash The hash of the board position
     * @return The best move or null if not found
     */
    public Move getBestMove(long boardHash) {
        TableEntry entry = table.get(boardHash);
        return entry != null ? entry.bestMove : null;
    }

    /**
     * Clears the table
     */
    public void clear() {
        table.clear();
    }

    /**
     * Returns the current size of the table
     */
    public int size() {
        return table.size();
    }
}