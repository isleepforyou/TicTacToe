import java.util.ArrayList;
import java.util.List;

/**
 * Génère les coups valides pour Ultimate Tic-Tac-Toe
 */
public class MoveGenerator {
    /**
     * Génère tous les coups valides pour la position actuelle
     */
    public static List<Move> generateMoves(Board board) {
        List<Move> validMoves = new ArrayList<>();
        int nextLocalBoard = board.getNextLocalBoard();
        int[][] boardState = board.getBoard();
        int[] localBoardStatus = board.getLocalBoardStatus();

        // Détermine les plateaux locaux à examiner
        List<Integer> boardsToCheck = new ArrayList<>();

        // Si nextLocalBoard est -1, joueur peut jouer partout (dans les plateaux non fermés)
        if (nextLocalBoard == -1) {
            for (int localBoard = 0; localBoard < 9; localBoard++) {
                if (localBoardStatus[localBoard] == 0) {
                    boardsToCheck.add(localBoard);
                }
            }
        } else {
            // Joueur doit jouer dans le plateau spécifié
            boardsToCheck.add(nextLocalBoard);
        }

        // Parcours des plateaux à examiner
        for (int localBoard : boardsToCheck) {
            int startRow = (localBoard / 3) * 3;
            int startCol = (localBoard % 3) * 3;

            // Génère les coups possibles dans ce plateau local
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

    /**
     * Convertit un coup textuel comme "D6" en indices ligne et colonne
     */
    public static Move parseMove(String moveStr) {
        if (moveStr == null || moveStr.length() < 2) {
            return null;
        }

        char colChar = moveStr.charAt(0);
        char rowChar = moveStr.charAt(1);

        int col = colChar - 'A';
        int row = '9' - rowChar;

        // Valide le coup
        if (col < 0 || col > 8 || row < 0 || row > 8) {
            return null;
        }

        return new Move(row, col);
    }

    /**
     * Convertit indices en chaîne comme "D6"
     */
    public static String formatMove(Move move) {
        char colChar = (char)('A' + move.getCol());
        char rowChar = (char)('9' - move.getRow());

        return "" + colChar + rowChar;
    }
}