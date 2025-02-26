public class BoardEvaluator {
    private Mark cpuMark;
    private Mark opponentMark;

    public BoardEvaluator(Mark cpuMark, Mark opponentMark) {
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
    }

    public int evaluate(Board board) {
        Mark[][] localBoards = board.getLocalBoards();
        int score = 0;

        // Points pour les plateaux locaux gagnés
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (localBoards[r][c] == cpuMark) {
                    score += 10;
                    // Bonus pour positions stratégiques
                    if (r == 1 && c == 1) score += 5; // Centre
                    if ((r == 0 && c == 0) || (r == 0 && c == 2) ||
                            (r == 2 && c == 0) || (r == 2 && c == 2)) {
                        score += 3; // Coins
                    }
                } else if (localBoards[r][c] == opponentMark) {
                    score -= 10;
                    if (r == 1 && c == 1) score -= 5;
                    if ((r == 0 && c == 0) || (r == 0 && c == 2) ||
                            (r == 2 && c == 0) || (r == 2 && c == 2)) {
                        score -= 3;
                    }
                }
            }
        }

        // Évaluer les lignes, colonnes et diagonales globales
        score += evaluateGlobalLines(localBoards);

        // Évaluer les sous-plateaux non gagnés
        for (int lr = 0; lr < 3; lr++) {
            for (int lc = 0; lc < 3; lc++) {
                if (localBoards[lr][lc] == Mark.EMPTY) {
                    score += evaluateLocalBoard(board, lr, lc);
                }
            }
        }

        // Évaluer l'avantage du prochain coup
        int nextLocalRow = board.getNextLocalRow();
        int nextLocalCol = board.getNextLocalCol();

        if (nextLocalRow != -1 && nextLocalCol != -1) {
            // Vérifier si le prochain plateau est avantageux
            if (hasWinningThreat(board, nextLocalRow, nextLocalCol, opponentMark)) {
                score -= 8; // Malus important si l'adversaire peut gagner un plateau
            }
        }

        return score;
    }

    // Méthodes d'évaluation détaillées
    private int evaluateGlobalLines(Mark[][] localBoards) {
        int score = 0;

        // Lignes
        for (int r = 0; r < 3; r++) {
            int markCount = 0, opponentCount = 0, emptyCount = 0;
            for (int c = 0; c < 3; c++) {
                if (localBoards[r][c] == cpuMark) markCount++;
                else if (localBoards[r][c] == opponentMark) opponentCount++;
                else emptyCount++;
            }

            if (markCount == 2 && emptyCount == 1) score += 5;
            if (opponentCount == 2 && emptyCount == 1) score -= 7;
        }

        return score;
    }

    private int evaluateLocalBoard(Board board, int localRow, int localCol) {
        int score = 0;

        // Vérifier les menaces imminentes
        if (hasWinningThreat(board, localRow, localCol, cpuMark)) {
            score += 3;
        }

        if (hasWinningThreat(board, localRow, localCol, opponentMark)) {
            score -= 4;
        }

        // Evaluer le centre du plateau local
        Mark[][] grid = board.getGrid();
        if (grid[localRow * 3 + 1][localCol * 3 + 1] == cpuMark) {
            score += 2;
        } else if (grid[localRow * 3 + 1][localCol * 3 + 1] == opponentMark) {
            score -= 2;
        }

        return score;
    }

    // Détection de menaces
    public boolean hasWinningThreat(Board board, int localRow, int localCol, Mark mark) {
        // Détection de menaces de gagner un plateau local
        // (implémentation comme avant)
        // ...

        return false; // Placeholder
    }

    // Vérification des coups gagnants
    public boolean isWinningMove(Board board, Move move, Mark mark) {
        // Vérification si un coup gagne directement un plateau local
        // (implémentation comme avant)
        // ...

        return false; // Placeholder
    }
}