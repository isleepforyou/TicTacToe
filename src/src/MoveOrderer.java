import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MoveOrderer {
    private Mark cpuMark;
    private Mark opponentMark;
    private BoardEvaluator evaluator;
    private Random random;

    // Pour mémoriser les coups "tueurs"
    private Move[] killerMoves;
    private static final int KILLER_SLOTS = 2;

    // Pour mémoriser l'historique des coups
    private Map<String, Integer> moveHistory;

    // Poids pour l'ordonnancement des coups
    private static final int WINNING_MOVE_PRIORITY = 10000;    // Coup gagnant direct
    private static final int BLOCK_WINNING_MOVE_PRIORITY = 9000; // Blocage d'un coup gagnant
    private static final int CAPTURE_BOARD_PRIORITY = 8000;    // Gagner un plateau local
    private static final int CENTER_GLOBAL_PRIORITY = 5000;    // Jouer au centre global
    private static final int CORNER_GLOBAL_PRIORITY = 4000;    // Jouer dans un coin global
    private static final int CREATES_FORK_PRIORITY = 3000;     // Créer une fourche
    private static final int BLOCKS_FORK_PRIORITY = 2500;      // Bloquer une fourche
    private static final int STRATEGIC_NEXT_PRIORITY = 2000;   // Envoyer dans un plateau stratégique
    private static final int CENTER_LOCAL_PRIORITY = 1000;     // Jouer au centre local
    private static final int CORNER_LOCAL_PRIORITY = 500;      // Jouer dans un coin local
    private static final int KILLER_MOVE_BONUS = 3000;         // Bonus pour les coups "tueurs"
    private static final int HISTORY_BONUS = 50;               // Bonus pour les coups historiques

    public MoveOrderer(Mark cpuMark, Mark opponentMark) {
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
        this.evaluator = new BoardEvaluator(cpuMark, opponentMark);
        this.random = new Random();
        this.killerMoves = new Move[KILLER_SLOTS];
        this.moveHistory = new HashMap<>();
    }

    public void sortMoves(ArrayList<Move> moves, Board board, Mark currentPlayer) {
        try {
            // Définir les priorités des coups
            ArrayList<MoveWithPriority> prioritizedMoves = new ArrayList<>();

            for (Move move : moves) {
                int priority = calculateMovePriority(move, board, currentPlayer);

                // Ajouter un petit aléatoire pour éviter les comportements mécaniques
                priority += random.nextInt(10);

                prioritizedMoves.add(new MoveWithPriority(move, priority));
            }

            // Trier les coups par priorité décroissante
            Collections.sort(prioritizedMoves, Comparator.comparingInt(MoveWithPriority::getPriority).reversed());

            // Remplacer la liste originale par la liste triée
            moves.clear();
            for (MoveWithPriority prioritizedMove : prioritizedMoves) {
                moves.add(prioritizedMove.getMove());
            }
        } catch (Exception e) {
            System.err.println("Erreur dans sortMoves: " + e.getMessage());
            // En cas d'erreur, laisser la liste intacte
        }
    }

    private int calculateMovePriority(Move move, Board board, Mark currentPlayer) {
        if (board == null) {
            return 0; // Protection contre les erreurs
        }

        Mark opponentPlayer = (currentPlayer == Mark.X) ? Mark.O : Mark.X;
        int priority = 0;

        try {
            // 1. Coups gagnants
            if (evaluator.isWinningMove(board, move, currentPlayer)) {
                return WINNING_MOVE_PRIORITY;
            }

            // 2. Bloquer un coup gagnant de l'adversaire
            Board tempBoard = board.copy();
            tempBoard.play(move, opponentPlayer);
            if (evaluator.isWinningMove(tempBoard, move, opponentPlayer)) {
                return BLOCK_WINNING_MOVE_PRIORITY;
            }

            // 3. Gagner un plateau local
            tempBoard = board.copy();
            tempBoard.play(move, currentPlayer);
            if (tempBoard.getLocalBoards()[move.getGlobalRow()][move.getGlobalCol()] == currentPlayer) {
                priority += CAPTURE_BOARD_PRIORITY;
            }

            // 4. Position stratégique au niveau global
            int globalRow = move.getGlobalRow();
            int globalCol = move.getGlobalCol();

            if (globalRow == 1 && globalCol == 1) {
                priority += CENTER_GLOBAL_PRIORITY; // Centre global
            } else if ((globalRow == 0 || globalRow == 2) && (globalCol == 0 || globalCol == 2)) {
                priority += CORNER_GLOBAL_PRIORITY; // Coins globaux
            }

            // 5. Position stratégique au niveau local
            int localRow = move.getLocalRow();
            int localCol = move.getLocalCol();

            if (localRow == 1 && localCol == 1) {
                priority += CENTER_LOCAL_PRIORITY; // Centre local
            } else if ((localRow == 0 || localRow == 2) && (localCol == 0 || localCol == 2)) {
                priority += CORNER_LOCAL_PRIORITY; // Coins locaux
            }

            // 6. Fourches et menaces
            tempBoard = board.copy();
            tempBoard.play(move, currentPlayer);
            int forkCount = countForkOpportunities(tempBoard, move.getGlobalRow(), move.getGlobalCol(), currentPlayer);
            if (forkCount >= 2) {
                priority += CREATES_FORK_PRIORITY;
            }

            tempBoard = board.copy();
            tempBoard.play(move, opponentPlayer);
            int opponentForkCount = countForkOpportunities(tempBoard, move.getGlobalRow(), move.getGlobalCol(), opponentPlayer);
            if (opponentForkCount >= 2) {
                priority += BLOCKS_FORK_PRIORITY;
            }

            // 7. Envoyer vers un plateau stratégique
            if (isBoardStrategic(board, localRow, localCol, currentPlayer)) {
                priority += STRATEGIC_NEXT_PRIORITY;
            }

            // 8. Coup "tueur"
            if (isKillerMove(move)) {
                priority += KILLER_MOVE_BONUS;
            }

            // 9. Historique des coups
            String moveKey = move.getRow() + "," + move.getCol() + "," + currentPlayer;
            if (moveHistory.containsKey(moveKey)) {
                int historyValue = moveHistory.get(moveKey);
                priority += Math.min(historyValue, HISTORY_BONUS);
            }

        } catch (Exception e) {
            System.err.println("Erreur dans calculateMovePriority: " + e.getMessage());
        }

        return priority;
    }

    // Compter les opportunités de fourche dans un plateau local
    private int countForkOpportunities(Board board, int localRow, int localCol, Mark mark) {
        try {
            // Vérifier si l'évaluateur existe
            if (evaluator == null) {
                System.err.println("Evaluator est null dans countForkOpportunities");
                return 0;
            }

            // Compter les menaces de victoire
            return evaluator.hasWinningThreat(board, localRow, localCol, mark) ? 1 : 0;
        } catch (Exception e) {
            System.err.println("Erreur dans countForkOpportunities: " + e.getMessage());
            return 0;
        }
    }

    // Vérifier si un plateau est stratégique
    private boolean isBoardStrategic(Board board, int localRow, int localCol, Mark player) {
        if (board == null || localRow < 0 || localCol < 0 || localRow >= 3 || localCol >= 3) {
            return false;
        }

        // Un plateau est stratégique s'il est au centre ou dans un coin
        if (localRow == 1 && localCol == 1) {
            return true;  // Centre
        }

        if ((localRow == 0 || localRow == 2) && (localCol == 0 || localCol == 2)) {
            return true;  // Coin
        }

        return false;
    }

    // Vérifier si un coup est dans la liste des coups "tueurs"
    private boolean isKillerMove(Move move) {
        for (Move killerMove : killerMoves) {
            if (killerMove != null &&
                    killerMove.getRow() == move.getRow() &&
                    killerMove.getCol() == move.getCol()) {
                return true;
            }
        }
        return false;
    }

    // Ajouter un coup à la liste des coups "tueurs"
    public void addKillerMove(Move move) {
        if (move == null) return;

        // Décaler les coups existants
        for (int i = killerMoves.length - 1; i > 0; i--) {
            killerMoves[i] = killerMoves[i-1];
        }

        // Ajouter le nouveau coup en première position
        killerMoves[0] = new Move(move.getRow(), move.getCol());
    }

    // Mettre à jour l'historique des coups
    public void updateMoveHistory(Move move, Mark player, int depth) {
        if (move == null) return;

        String moveKey = move.getRow() + "," + move.getCol() + "," + player;
        int currentValue = moveHistory.getOrDefault(moveKey, 0);
        moveHistory.put(moveKey, currentValue + depth);
    }

    // Classe interne pour associer un coup à sa priorité
    private class MoveWithPriority {
        private Move move;
        private int priority;

        public MoveWithPriority(Move move, int priority) {
            this.move = move;
            this.priority = priority;
        }

        public Move getMove() {
            return move;
        }

        public int getPriority() {
            return priority;
        }
    }
}