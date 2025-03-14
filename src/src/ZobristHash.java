import java.util.Random;

/**
 * Implements Zobrist hashing for board positions in Ultimate Tic-Tac-Toe
 * This allows fast generation of unique hashes for board positions
 */
public class ZobristHash {
    // Random numbers for each possible state of each cell
    // [row][col][piece] where piece is 0=empty, 1=O, 2=X
    private long[][][] zobristTable;

    // Random values for next local board states
    private long[] nextLocalBoardTable;

    /**
     * Initializes the Zobrist hash table with random values
     */
    public ZobristHash() {
        Random random = new Random(42); // Fixed seed for reproducibility

        // Initialize table for 9x9 board with 3 possibilities per cell (empty, O, X)
        zobristTable = new long[9][9][3];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                for (int k = 0; k < 3; k++) {
                    zobristTable[i][j][k] = random.nextLong();
                }
            }
        }

        // Initialize table for the next local board (-1 to 8)
        nextLocalBoardTable = new long[10]; // -1 plus 0-8
        for (int i = 0; i < 10; i++) {
            nextLocalBoardTable[i] = random.nextLong();
        }
    }

    /**
     * Computes the Zobrist hash for a board position
     *
     * @param board The game board
     * @return The hash value
     */
    public long computeHash(Board board) {
        long hash = 0;
        int[][] boardState = board.getBoard();

        // XOR all the pieces on the board
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                int piece = boardState[i][j];
                int pieceIndex;

                // Convert piece value to index (0=empty, 1=O, 2=X)
                if (piece == 0) {
                    pieceIndex = 0;
                } else if (piece == 2) { // O
                    pieceIndex = 1;
                } else { // X (4)
                    pieceIndex = 2;
                }

                hash ^= zobristTable[i][j][pieceIndex];
            }
        }

        // XOR the next local board
        int nextLocal = board.getNextLocalBoard();
        hash ^= nextLocalBoardTable[nextLocal + 1]; // +1 because nextLocal can be -1

        return hash;
    }

    /**
     * Updates the hash when a move is made
     *
     * @param hash The current hash
     * @param row The row where the piece is placed
     * @param col The column where the piece is placed
     * @param piece The piece being placed (2 for O, 4 for X)
     * @param oldNextLocal The previous next local board
     * @param newNextLocal The new next local board
     * @return The updated hash
     */
    public long updateHash(long hash, int row, int col, int piece, int oldNextLocal, int newNextLocal) {
        // Remove the old empty cell from the hash
        hash ^= zobristTable[row][col][0];

        // Add the new piece to the hash
        int pieceIndex = (piece == 2) ? 1 : 2;
        hash ^= zobristTable[row][col][pieceIndex];

        // Update the next local board in the hash
        hash ^= nextLocalBoardTable[oldNextLocal + 1];
        hash ^= nextLocalBoardTable[newNextLocal + 1];

        return hash;
    }
}