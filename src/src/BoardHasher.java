class BoardHasher {
    private static final long SEED = 0x7FFFFFFF;
    private static final long[][] ZOBRIST_TABLE_X = new long[9][9];
    private static final long[][] ZOBRIST_TABLE_O = new long[9][9];
    private static final long MAXIMIZING_HASH = 0x123456789ABCDEFL;

    static {
        // Initialiser les tables Zobrist avec des valeurs pseudo-aléatoires
        java.util.Random rand = new java.util.Random(SEED);
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                ZOBRIST_TABLE_X[r][c] = rand.nextLong();
                ZOBRIST_TABLE_O[r][c] = rand.nextLong();
            }
        }
    }

    public static long hash(Board board, boolean isMaximizing) {
        Mark[][] grid = board.getGrid();
        long hash = 0;

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (grid[r][c] == Mark.X) {
                    hash ^= ZOBRIST_TABLE_X[r][c];
                } else if (grid[r][c] == Mark.O) {
                    hash ^= ZOBRIST_TABLE_O[r][c];
                }
            }
        }

        // Inclure l'information sur le joueur à qui c'est le tour
        if (isMaximizing) {
            hash ^= MAXIMIZING_HASH;
        }

        return hash;
    }
}