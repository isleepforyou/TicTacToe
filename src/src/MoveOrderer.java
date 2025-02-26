import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MoveOrderer {
    private Mark cpuMark;
    private Mark opponentMark;
    private BoardEvaluator evaluator;

    public MoveOrderer(Mark cpuMark, Mark opponentMark) {
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
        this.evaluator = new BoardEvaluator(cpuMark, opponentMark);
    }

    public void sortMoves(ArrayList<Move> moves, Board board, Mark currentPlayer) {
        // Identifier les coups gagnants
        ArrayList<Move> winningMoves = new ArrayList<>();
        for (Move move : moves) {
            if (evaluator.isWinningMove(board, move, currentPlayer)) {
                winningMoves.add(move);
            }
        }

        if (!winningMoves.isEmpty()) {
            moves.clear();
            moves.addAll(winningMoves);
            return;
        }

        // Tri par valeur stratégique
        Collections.sort(moves, new Comparator<Move>() {
            @Override
            public int compare(Move m1, Move m2) {
                int strategic1 = getStrategicValue(m1);
                int strategic2 = getStrategicValue(m2);

                if (strategic1 != strategic2) {
                    return strategic2 - strategic1;
                }

                return 0;
            }
        });
    }

    private int getStrategicValue(Move move) {
        int globalRow = move.getGlobalRow();
        int globalCol = move.getGlobalCol();
        int localRow = move.getLocalRow();
        int localCol = move.getLocalCol();

        int value = 0;

        // Valoriser les plateaux locaux stratégiques
        if (globalRow == 1 && globalCol == 1) value += 3; // Centre global
        else if ((globalRow == 0 || globalRow == 2) && (globalCol == 0 || globalCol == 2)) value += 2; // Coins globaux

        // Valoriser les positions locales stratégiques
        if (localRow == 1 && localCol == 1) value += 2; // Centre local
        else if ((localRow == 0 || localRow == 2) && (localCol == 0 || localCol == 2)) value += 1; // Coins locaux

        return value;
    }
}