import java.util.concurrent.ConcurrentHashMap;

public class TranspositionTable {
    private ConcurrentHashMap<Long, TranspositionEntry> table;

    public TranspositionTable() {
        table = new ConcurrentHashMap<>(500000);
    }

    public void put(long hash, int score, int depth) {
        table.put(hash, new TranspositionEntry(score, depth));
    }

    public TranspositionEntry get(long hash) {
        return table.get(hash);
    }

    public void clear() {
        table.clear();
    }

    // Fonction de hachage
    public long hash(Board board, boolean isMaximizing) {
        Mark[][] grid = board.getGrid();
        long hash = isMaximizing ? 1L : 2L;

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                hash = hash * 31 + (grid[r][c] == Mark.EMPTY ? 0 : (grid[r][c] == Mark.X ? 1 : 2));
            }
        }

        return hash;
    }
}