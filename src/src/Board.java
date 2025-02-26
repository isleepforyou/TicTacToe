import java.util.ArrayList;

class Board {
    private Mark[][] grid;
    private Mark[][] localBoards;
    private int nextLocalRow;
    private int nextLocalCol;

    public Board() {
        // Initialisation d'un plateau 9x9
        grid = new Mark[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                grid[i][j] = Mark.EMPTY;
            }
        }

        // Suivi de l'état des 9 plateaux locaux (3x3)
        localBoards = new Mark[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                localBoards[i][j] = Mark.EMPTY;
            }
        }

        // Au début, on peut jouer n'importe où
        nextLocalRow = -1;
        nextLocalCol = -1;
    }

    public void play(Move m, Mark mark) {
        int r = m.getRow();
        int c = m.getCol();

        // Vérifier si le coup est valide
        if (r >= 0 && r < 9 && c >= 0 && c < 9 && grid[r][c] == Mark.EMPTY) {
            // Vérifier si le coup est dans le bon plateau local
            if (isValidLocalBoard(m.getGlobalRow(), m.getGlobalCol())) {
                grid[r][c] = mark;

                // Mettre à jour l'état du plateau local
                updateLocalBoardState(m.getGlobalRow(), m.getGlobalCol());

                // Définir le prochain plateau local où jouer
                nextLocalRow = m.getLocalRow();
                nextLocalCol = m.getLocalCol();

                // Si le prochain plateau local est fermé (gagné ou plein), le joueur peut jouer n'importe où
                if (isLocalBoardClosed(nextLocalRow, nextLocalCol)) {
                    nextLocalRow = -1;
                    nextLocalCol = -1;
                }
            }
        }
    }

    private boolean isValidLocalBoard(int localRow, int localCol) {
        // Si aucun plateau local n'est spécifié, tous sont valides s'ils ne sont pas fermés
        if (nextLocalRow == -1 && nextLocalCol == -1) {
            return !isLocalBoardClosed(localRow, localCol);
        }

        // Sinon, vérifier si c'est le plateau local attendu
        return (localRow == nextLocalRow && localCol == nextLocalCol);
    }

    private boolean isLocalBoardClosed(int localRow, int localCol) {
        // Un plateau local est fermé s'il est gagné ou plein
        return localBoards[localRow][localCol] != Mark.EMPTY || isLocalBoardFull(localRow, localCol);
    }

    private boolean isLocalBoardFull(int localRow, int localCol) {
        for (int r = localRow * 3; r < (localRow + 1) * 3; r++) {
            for (int c = localCol * 3; c < (localCol + 1) * 3; c++) {
                if (grid[r][c] == Mark.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    private void updateLocalBoardState(int localRow, int localCol) {
        Mark winner = checkLocalBoardWinner(localRow, localCol);
        if (winner != Mark.EMPTY) {
            localBoards[localRow][localCol] = winner;
        }
    }

    // Vérifier s'il y a un gagnant dans un plateau local
    private Mark checkLocalBoardWinner(int localRow, int localCol) {
        // Vérifier les lignes
        for (int r = 0; r < 3; r++) {
            int row = localRow * 3 + r;
            if (grid[row][localCol * 3] != Mark.EMPTY &&
                    grid[row][localCol * 3] == grid[row][localCol * 3 + 1] &&
                    grid[row][localCol * 3 + 1] == grid[row][localCol * 3 + 2]) {
                return grid[row][localCol * 3];
            }
        }

        // Vérifier les colonnes
        for (int c = 0; c < 3; c++) {
            int col = localCol * 3 + c;
            if (grid[localRow * 3][col] != Mark.EMPTY &&
                    grid[localRow * 3][col] == grid[localRow * 3 + 1][col] &&
                    grid[localRow * 3 + 1][col] == grid[localRow * 3 + 2][col]) {
                return grid[localRow * 3][col];
            }
        }

        // Vérifier les diagonales
        int center = localRow * 3 + 1, centerCol = localCol * 3 + 1;
        if (grid[localRow * 3][localCol * 3] != Mark.EMPTY &&
                grid[localRow * 3][localCol * 3] == grid[center][centerCol] &&
                grid[center][centerCol] == grid[localRow * 3 + 2][localCol * 3 + 2]) {
            return grid[localRow * 3][localCol * 3];
        }

        if (grid[localRow * 3][localCol * 3 + 2] != Mark.EMPTY &&
                grid[localRow * 3][localCol * 3 + 2] == grid[center][centerCol] &&
                grid[center][centerCol] == grid[localRow * 3 + 2][localCol * 3]) {
            return grid[localRow * 3][localCol * 3 + 2];
        }

        return Mark.EMPTY;
    }

    public boolean isTerminal() {
        // Le jeu est terminé s'il y a un gagnant global
        if (checkWinner() != Mark.EMPTY) {
            return true;
        }

        // Ou si tous les plateaux locaux sont fermés
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (!isLocalBoardClosed(r, c)) {
                    return false;
                }
            }
        }

        return true;
    }

    // Vérifier s'il y a un gagnant global
    private Mark checkWinner() {
        // Vérifier les lignes
        for (int r = 0; r < 3; r++) {
            if (localBoards[r][0] != Mark.EMPTY &&
                    localBoards[r][0] == localBoards[r][1] &&
                    localBoards[r][1] == localBoards[r][2]) {
                return localBoards[r][0];
            }
        }

        // Vérifier les colonnes
        for (int c = 0; c < 3; c++) {
            if (localBoards[0][c] != Mark.EMPTY &&
                    localBoards[0][c] == localBoards[1][c] &&
                    localBoards[1][c] == localBoards[2][c]) {
                return localBoards[0][c];
            }
        }

        // Vérifier les diagonales
        if (localBoards[0][0] != Mark.EMPTY &&
                localBoards[0][0] == localBoards[1][1] &&
                localBoards[1][1] == localBoards[2][2]) {
            return localBoards[0][0];
        }

        if (localBoards[0][2] != Mark.EMPTY &&
                localBoards[0][2] == localBoards[1][1] &&
                localBoards[1][1] == localBoards[2][0]) {
            return localBoards[0][2];
        }

        return Mark.EMPTY;
    }

    public int evaluate(Mark mark) {
        Mark winner = checkWinner();
        if (winner == mark) {
            return 100; // Victoire
        } else if (winner != Mark.EMPTY) {
            return -100; // Défaite
        } else if (isTerminal()) {
            return 0; // Match nul
        }

        // Évaluation heuristique
        Mark opponent = (mark == Mark.X) ? Mark.O : Mark.X;
        int score = 0;

        // Valeur des plateaux locaux gagnés
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (localBoards[r][c] == mark) {
                    score += 10;
                    // Bonus pour les positions stratégiques
                    if ((r == 0 && c == 0) || (r == 0 && c == 2) ||
                            (r == 2 && c == 0) || (r == 2 && c == 2)) {
                        score += 2; // Coins
                    }
                    if (r == 1 && c == 1) {
                        score += 3; // Centre
                    }
                } else if (localBoards[r][c] == opponent) {
                    score -= 10;
                    // Malus pour les positions stratégiques adverses
                    if ((r == 0 && c == 0) || (r == 0 && c == 2) ||
                            (r == 2 && c == 0) || (r == 2 && c == 2)) {
                        score -= 2; // Coins
                    }
                    if (r == 1 && c == 1) {
                        score -= 3; // Centre
                    }
                } else {
                    // Évaluation des plateaux locaux non gagnés
                    int localScore = evaluateLocalBoard(r, c, mark);
                    score += localScore;
                }
            }
        }

        // Bonus pour forcer l'adversaire à jouer dans des plateaux désavantageux
        if (nextLocalRow != -1 && nextLocalCol != -1) {
            if (isLocalBoardAdvantage(nextLocalRow, nextLocalCol, opponent)) {
                score -= 5;
            }
        }

        return score;
    }

    private boolean isLocalBoardAdvantage(int localRow, int localCol, Mark player) {
        // Un plateau est avantageux si le joueur a des options pour gagner
        int playerLines = 0;

        // Vérifier les lignes
        for (int r = 0; r < 3; r++) {
            int count = 0;
            for (int c = 0; c < 3; c++) {
                int row = localRow * 3 + r;
                int col = localCol * 3 + c;
                if (grid[row][col] == player) count++;
                else if (grid[row][col] != Mark.EMPTY) count = -100; // Ligne bloquée
            }
            if (count == 2) playerLines++;
        }

        // Vérifier les colonnes
        for (int c = 0; c < 3; c++) {
            int count = 0;
            for (int r = 0; r < 3; r++) {
                int row = localRow * 3 + r;
                int col = localCol * 3 + c;
                if (grid[row][col] == player) count++;
                else if (grid[row][col] != Mark.EMPTY) count = -100;
            }
            if (count == 2) playerLines++;
        }

        // Vérifier les diagonales
        int diag1 = 0, diag2 = 0;
        for (int i = 0; i < 3; i++) {
            if (grid[localRow * 3 + i][localCol * 3 + i] == player) diag1++;
            else if (grid[localRow * 3 + i][localCol * 3 + i] != Mark.EMPTY) diag1 = -100;

            if (grid[localRow * 3 + i][localCol * 3 + 2 - i] == player) diag2++;
            else if (grid[localRow * 3 + i][localCol * 3 + 2 - i] != Mark.EMPTY) diag2 = -100;
        }
        if (diag1 == 2) playerLines++;
        if (diag2 == 2) playerLines++;

        return playerLines > 0;
    }

    private int evaluateLocalBoard(int localRow, int localCol, Mark mark) {
        int score = 0;
        Mark opponent = (mark == Mark.X) ? Mark.O : Mark.X;

        // Évaluer les lignes
        for (int r = 0; r < 3; r++) {
            int markCount = 0, opponentCount = 0, emptyCount = 0;
            for (int c = 0; c < 3; c++) {
                int row = localRow * 3 + r;
                int col = localCol * 3 + c;
                if (grid[row][col] == mark) markCount++;
                else if (grid[row][col] == opponent) opponentCount++;
                else emptyCount++;
            }

            score += evaluateLineScore(markCount, opponentCount, emptyCount);
        }

        // Évaluer les colonnes
        for (int c = 0; c < 3; c++) {
            int markCount = 0, opponentCount = 0, emptyCount = 0;
            for (int r = 0; r < 3; r++) {
                int row = localRow * 3 + r;
                int col = localCol * 3 + c;
                if (grid[row][col] == mark) markCount++;
                else if (grid[row][col] == opponent) opponentCount++;
                else emptyCount++;
            }

            score += evaluateLineScore(markCount, opponentCount, emptyCount);
        }

        // Évaluer les diagonales
        int markCount1 = 0, opponentCount1 = 0, emptyCount1 = 0;
        int markCount2 = 0, opponentCount2 = 0, emptyCount2 = 0;

        for (int i = 0; i < 3; i++) {
            // Diagonale 1
            if (grid[localRow * 3 + i][localCol * 3 + i] == mark) markCount1++;
            else if (grid[localRow * 3 + i][localCol * 3 + i] == opponent) opponentCount1++;
            else emptyCount1++;

            // Diagonale 2
            if (grid[localRow * 3 + i][localCol * 3 + 2 - i] == mark) markCount2++;
            else if (grid[localRow * 3 + i][localCol * 3 + 2 - i] == opponent) opponentCount2++;
            else emptyCount2++;
        }

        score += evaluateLineScore(markCount1, opponentCount1, emptyCount1);
        score += evaluateLineScore(markCount2, opponentCount2, emptyCount2);

        return score;
    }

    private int evaluateLineScore(int markCount, int opponentCount, int emptyCount) {
        if (markCount == 3) return 10; // Ligne gagnée
        if (opponentCount == 3) return -10; // Ligne perdue

        if (markCount == 2 && emptyCount == 1) return 3; // Opportunité de gagner
        if (opponentCount == 2 && emptyCount == 1) return -3; // Menace à bloquer

        if (markCount == 1 && emptyCount == 2) return 1; // Début de ligne
        if (opponentCount == 1 && emptyCount == 2) return -1; // Début de ligne adverse

        return 0;
    }

    public ArrayList<Move> getAvailableMoves() {
        ArrayList<Move> moves = new ArrayList<>();

        // Si le joueur doit jouer dans un plateau local spécifique
        if (nextLocalRow != -1 && nextLocalCol != -1) {
            // Si ce plateau est fermé, on peut jouer n'importe où
            if (isLocalBoardClosed(nextLocalRow, nextLocalCol)) {
                for (int lr = 0; lr < 3; lr++) {
                    for (int lc = 0; lc < 3; lc++) {
                        if (!isLocalBoardClosed(lr, lc)) {
                            addMovesForLocalBoard(moves, lr, lc);
                        }
                    }
                }
            } else {
                // Sinon, on doit jouer dans ce plateau local
                addMovesForLocalBoard(moves, nextLocalRow, nextLocalCol);
            }
        } else {
            // Si on peut jouer n'importe où
            for (int lr = 0; lr < 3; lr++) {
                for (int lc = 0; lc < 3; lc++) {
                    if (!isLocalBoardClosed(lr, lc)) {
                        addMovesForLocalBoard(moves, lr, lc);
                    }
                }
            }
        }

        return moves;
    }

    private void addMovesForLocalBoard(ArrayList<Move> moves, int localRow, int localCol) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int row = localRow * 3 + r;
                int col = localCol * 3 + c;
                if (grid[row][col] == Mark.EMPTY) {
                    moves.add(new Move(row, col));
                }
            }
        }
    }

    // Méthode pour vérifier si le prochain plateau local est spécifié
    public boolean getNextLocalBoard() {
        return nextLocalRow != -1 && nextLocalCol != -1;
    }

    public Board copy() {
        Board newBoard = new Board();

        // Copier le plateau 9x9
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                newBoard.grid[r][c] = this.grid[r][c];
            }
        }

        // Copier les états des plateaux locaux
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                newBoard.localBoards[r][c] = this.localBoards[r][c];
            }
        }

        // Copier le prochain plateau local
        newBoard.nextLocalRow = this.nextLocalRow;
        newBoard.nextLocalCol = this.nextLocalCol;

        return newBoard;
    }

    public Mark[][] getGrid() {
        return grid;
    }

    // Ajout pour faciliter l'accès aux états locaux
    public Mark[][] getLocalBoards() {
        return localBoards;
    }

    public int getNextLocalRow() {
        return nextLocalRow;
    }

    public int getNextLocalCol() {
        return nextLocalCol;
    }
}