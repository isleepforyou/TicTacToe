import java.util.ArrayList;
import java.util.List;

/**
 * Generates valid moves for the Ultimate Tic-Tac-Toe game
 */
public class MoveGenerator {
    // Generate all valid moves for the current board state
    public static List<Move> generateMoves(Board board) {
        List<Move> validMoves = new ArrayList<>();
        int nextLocalBoard = board.getNextLocalBoard();
        int[][] boardState = board.getBoard();
        int[] localBoardStatus = board.getLocalBoardStatus();

        // If nextLocalBoard is -1, the player can play anywhere on any open local board
        if (nextLocalBoard == -1) {
            for (int localBoard = 0; localBoard < 9; localBoard++) {
                // Skip closed local boards
                if (localBoardStatus[localBoard] != 0) {
                    continue;
                }

                int startRow = (localBoard / 3) * 3;
                int startCol = (localBoard % 3) * 3;

                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        int row = startRow + i;
                        int col = startCol + j;

                        if (boardState[row][col] == 0) {
                            validMoves.add(new Move(row, col));
                        }
                    }
                }
            }
        } else {
            // The player must play in the specified local board
            int startRow = (nextLocalBoard / 3) * 3;
            int startCol = (nextLocalBoard % 3) * 3;

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    int row = startRow + i;
                    int col = startCol + j;

                    if (boardState[row][col] == 0) {
                        validMoves.add(new Move(row, col));
                    }
                }
            }
        }

        return validMoves;
    }

    // Convert a move like "D6" to row and column indices
    public static Move parseMove(String moveStr) {
        if (moveStr == null || moveStr.length() < 2) {
            return null;
        }

        char colChar = moveStr.charAt(0);
        char rowChar = moveStr.charAt(1);

        int col = colChar - 'A';
        int row = '9' - rowChar;

        // Validate the move
        if (col < 0 || col > 8 || row < 0 || row > 8) {
            return null;
        }

        return new Move(row, col);
    }

    // Convert row and column indices to a move string like "D6"
    public static String formatMove(Move move) {
        char colChar = (char)('A' + move.getCol());
        char rowChar = (char)('9' - move.getRow());

        return "" + colChar + rowChar;
    }
}