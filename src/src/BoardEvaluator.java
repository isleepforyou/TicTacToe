public class BoardEvaluator {
    private Mark cpuMark;
    private Mark opponentMark;

    // Poids stratégiques ajustés
    private static final int WIN_SCORE = 5000;            // Score de victoire très élevé
    private static final int LOCAL_BOARD_WIN = 100;       // Gagner un plateau local est crucial
    private static final int GLOBAL_CENTER_BONUS = 50;    // Centre du plateau global
    private static final int GLOBAL_CORNER_BONUS = 30;    // Coins du plateau global
    private static final int GLOBAL_EDGE_BONUS = 20;      // Bords du plateau global
    private static final int LOCAL_CENTER_BONUS = 15;     // Centre d'un plateau local
    private static final int LOCAL_CORNER_BONUS = 10;     // Coins d'un plateau local
    private static final int TWO_IN_ROW_SCORE = 25;       // Deux marques alignées avec possibilité de gagner
    private static final int BLOCK_TWO_IN_ROW = 20;       // Bloquer deux marques alignées
    private static final int FORK_BONUS = 40;             // Créer une fourche (deux menaces)
    private static final int STRATEGIC_NEXT_BOARD = 35;   // Envoyer l'adversaire dans un plateau stratégique
    private static final int PREVENT_LOCAL_LOSS = 80;     // Éviter de perdre un plateau local
    private static final int CONTROL_FLOW_BONUS = 30;     // Contrôler le flux du jeu

    public BoardEvaluator(Mark cpuMark, Mark opponentMark) {
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
    }

    public int evaluate(Board board) {
        if (board == null) {
            System.err.println("ERREUR: Board null dans evaluate");
            return 0;
        }

        try {
            Mark[][] localBoards = board.getLocalBoards();
            Mark[][] grid = board.getGrid();
            int score = 0;
            int moveCount = countTotalMarks(grid);

            // Vérifier si le jeu est terminal
            Mark winner = checkGlobalWinner(localBoards);
            if (winner == cpuMark) {
                return WIN_SCORE + (81 - moveCount) * 10; // Bonus pour gagner rapidement
            } else if (winner == opponentMark) {
                return -WIN_SCORE - (81 - moveCount) * 10; // Pénalité pour perdre
            }

            // Points pour les plateaux locaux gagnés
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (localBoards[r][c] == cpuMark) {
                        score += LOCAL_BOARD_WIN;

                        // Bonus pour positions stratégiques
                        if (r == 1 && c == 1) {
                            score += GLOBAL_CENTER_BONUS;  // Centre global
                        } else if ((r == 0 || r == 2) && (c == 0 || c == 2)) {
                            score += GLOBAL_CORNER_BONUS;  // Coins globaux
                        } else {
                            score += GLOBAL_EDGE_BONUS;    // Bords globaux
                        }
                    } else if (localBoards[r][c] == opponentMark) {
                        score -= LOCAL_BOARD_WIN;

                        // Malus pour positions stratégiques perdues
                        if (r == 1 && c == 1) {
                            score -= GLOBAL_CENTER_BONUS;
                        } else if ((r == 0 || r == 2) && (c == 0 || c == 2)) {
                            score -= GLOBAL_CORNER_BONUS;
                        } else {
                            score -= GLOBAL_EDGE_BONUS;
                        }
                    }
                }
            }

            // Évaluer les lignes, colonnes et diagonales globales
            score += evaluateGlobalLines(localBoards);

            // Évaluer les plateaux locaux non gagnés
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
                // Vérifier si le plateau suivant est fermé ou désavantageux
                if (isLocalBoardClosed(board, nextLocalRow, nextLocalCol)) {
                    score -= CONTROL_FLOW_BONUS; // Mauvais de forcer l'adversaire à choisir
                }
                else if (canOpponentWinLocalBoard(board, nextLocalRow, nextLocalCol)) {
                    score -= PREVENT_LOCAL_LOSS; // Très mauvais d'envoyer vers un plateau gagnant pour l'adversaire
                }
                else if (hasWinningThreat(board, nextLocalRow, nextLocalCol, opponentMark)) {
                    score -= BLOCK_TWO_IN_ROW; // Mauvais d'envoyer vers un plateau où l'adversaire a un avantage
                }
                else if (hasWinningThreat(board, nextLocalRow, nextLocalCol, cpuMark)) {
                    score += STRATEGIC_NEXT_BOARD; // Bon d'envoyer vers un plateau où nous avons un avantage
                }
            } else {
                // Si on contrôle le jeu (l'adversaire peut jouer n'importe où)
                score += CONTROL_FLOW_BONUS;
            }

            return score;
        } catch (Exception e) {
            System.err.println("ERREUR dans evaluate: " + e.getMessage());
            return 0; // En cas d'erreur, retourner un score neutre
        }
    }

    // Compter le nombre total de marques sur le plateau
    private int countTotalMarks(Mark[][] grid) {
        int count = 0;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (grid[r][c] != Mark.EMPTY) {
                    count++;
                }
            }
        }
        return count;
    }

    // Vérifier si un plateau est fermé (gagné ou plein)
    private boolean isLocalBoardClosed(Board board, int localRow, int localCol) {
        if (localRow < 0 || localCol < 0 || localRow >= 3 || localCol >= 3) {
            return true;
        }

        Mark[][] localBoards = board.getLocalBoards();
        if (localBoards[localRow][localCol] != Mark.EMPTY) {
            return true; // Le plateau est déjà gagné
        }

        // Vérifier si le plateau est plein
        for (int r = localRow * 3; r < (localRow + 1) * 3; r++) {
            for (int c = localCol * 3; c < (localCol + 1) * 3; c++) {
                if (board.getGrid()[r][c] == Mark.EMPTY) {
                    return false; // Il y a encore au moins une case vide
                }
            }
        }
        return true; // Plateau plein
    }

    // Vérifier si l'adversaire peut gagner un plateau local en un seul coup
    private boolean canOpponentWinLocalBoard(Board board, int localRow, int localCol) {
        if (localRow < 0 || localCol < 0 || localRow >= 3 || localCol >= 3) {
            return false;
        }

        // Si le plateau est déjà fermé
        if (board.getLocalBoards()[localRow][localCol] != Mark.EMPTY) {
            return false;
        }

        return hasWinningThreat(board, localRow, localCol, opponentMark);
    }

    // Évaluer les lignes, colonnes et diagonales globales
    private int evaluateGlobalLines(Mark[][] localBoards) {
        int score = 0;

        // Évaluer les lignes
        for (int r = 0; r < 3; r++) {
            score += evaluateLine(localBoards[r][0], localBoards[r][1], localBoards[r][2]);
        }

        // Évaluer les colonnes
        for (int c = 0; c < 3; c++) {
            score += evaluateLine(localBoards[0][c], localBoards[1][c], localBoards[2][c]);
        }

        // Évaluer les diagonales
        score += evaluateLine(localBoards[0][0], localBoards[1][1], localBoards[2][2]);
        score += evaluateLine(localBoards[0][2], localBoards[1][1], localBoards[2][0]);

        return score;
    }

    // Évaluer une ligne de trois cases
    private int evaluateLine(Mark a, Mark b, Mark c) {
        int score = 0;

        // Compter les marques de chaque joueur
        int cpuCount = 0, opponentCount = 0, emptyCount = 0;

        if (a == cpuMark) cpuCount++;
        else if (a == opponentMark) opponentCount++;
        else emptyCount++;

        if (b == cpuMark) cpuCount++;
        else if (b == opponentMark) opponentCount++;
        else emptyCount++;

        if (c == cpuMark) cpuCount++;
        else if (c == opponentMark) opponentCount++;
        else emptyCount++;

        // Évaluer selon la composition
        if (cpuCount == 2 && emptyCount == 1) {
            score += TWO_IN_ROW_SCORE; // Deux marques alignées avec possibilité de gagner
        }

        if (opponentCount == 2 && emptyCount == 1) {
            score -= BLOCK_TWO_IN_ROW; // Bloquer l'adversaire
        }

        // Aussi évaluer les débuts de lignes
        if (cpuCount == 1 && emptyCount == 2) {
            score += 5;
        }

        if (opponentCount == 1 && emptyCount == 2) {
            score -= 3;
        }

        return score;
    }

    // Évaluer un plateau local
    private int evaluateLocalBoard(Board board, int localRow, int localCol) {
        int score = 0;
        Mark[][] grid = board.getGrid();

        // Vérifier les opportunités et menaces
        if (hasWinningThreat(board, localRow, localCol, cpuMark)) {
            score += TWO_IN_ROW_SCORE;
        }

        if (hasWinningThreat(board, localRow, localCol, opponentMark)) {
            score -= BLOCK_TWO_IN_ROW;
        }

        // Vérifier les opportunités de fourche
        int forkCount = countForkOpportunities(board, localRow, localCol, cpuMark);
        if (forkCount > 0) {
            score += FORK_BONUS * forkCount;
        }

        int opponentForkCount = countForkOpportunities(board, localRow, localCol, opponentMark);
        if (opponentForkCount > 0) {
            score -= FORK_BONUS * opponentForkCount;
        }

        // Évaluer les positions stratégiques
        int centerRow = localRow * 3 + 1;
        int centerCol = localCol * 3 + 1;

        if (grid[centerRow][centerCol] == cpuMark) {
            score += LOCAL_CENTER_BONUS;
        } else if (grid[centerRow][centerCol] == opponentMark) {
            score -= LOCAL_CENTER_BONUS;
        }

        // Évaluer les coins
        for (int r = 0; r < 3; r += 2) {
            for (int c = 0; c < 3; c += 2) {
                int row = localRow * 3 + r;
                int col = localCol * 3 + c;
                if (grid[row][col] == cpuMark) {
                    score += LOCAL_CORNER_BONUS;
                } else if (grid[row][col] == opponentMark) {
                    score -= LOCAL_CORNER_BONUS;
                }
            }
        }

        return score;
    }

    // Vérifier s'il y a un gagnant global
    private Mark checkGlobalWinner(Mark[][] localBoards) {
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

    // Détection des menaces de victoire
    public boolean hasWinningThreat(Board board, int localRow, int localCol, Mark mark) {
        Mark[][] grid = board.getGrid();

        // Vérifier les lignes
        for (int r = 0; r < 3; r++) {
            int markCount = 0, emptyCount = 0;
            for (int c = 0; c < 3; c++) {
                int row = localRow * 3 + r;
                int col = localCol * 3 + c;
                if (grid[row][col] == mark) {
                    markCount++;
                } else if (grid[row][col] == Mark.EMPTY) {
                    emptyCount++;
                }
            }
            if (markCount == 2 && emptyCount == 1) {
                return true;
            }
        }

        // Vérifier les colonnes
        for (int c = 0; c < 3; c++) {
            int markCount = 0, emptyCount = 0;
            for (int r = 0; r < 3; r++) {
                int row = localRow * 3 + r;
                int col = localCol * 3 + c;
                if (grid[row][col] == mark) {
                    markCount++;
                } else if (grid[row][col] == Mark.EMPTY) {
                    emptyCount++;
                }
            }
            if (markCount == 2 && emptyCount == 1) {
                return true;
            }
        }

        // Vérifier la première diagonale
        int markCount1 = 0, emptyCount1 = 0;
        for (int i = 0; i < 3; i++) {
            int row = localRow * 3 + i;
            int col = localCol * 3 + i;
            if (grid[row][col] == mark) {
                markCount1++;
            } else if (grid[row][col] == Mark.EMPTY) {
                emptyCount1++;
            }
        }
        if (markCount1 == 2 && emptyCount1 == 1) {
            return true;
        }

        // Vérifier la seconde diagonale
        int markCount2 = 0, emptyCount2 = 0;
        for (int i = 0; i < 3; i++) {
            int row = localRow * 3 + i;
            int col = localCol * 3 + (2 - i);
            if (grid[row][col] == mark) {
                markCount2++;
            } else if (grid[row][col] == Mark.EMPTY) {
                emptyCount2++;
            }
        }
        if (markCount2 == 2 && emptyCount2 == 1) {
            return true;
        }

        return false;
    }

    // Compter les opportunités de fourche (situations avec deux lignes potentielles)
    private int countForkOpportunities(Board board, int localRow, int localCol, Mark mark) {
        Mark[][] grid = board.getGrid();
        int count = 0;

        // Pour chaque case vide, vérifier si elle crée deux lignes potentielles
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int row = localRow * 3 + r;
                int col = localCol * 3 + c;

                if (grid[row][col] == Mark.EMPTY) {
                    // Simuler un coup sur cette case
                    grid[row][col] = mark;

                    // Compter les lignes potentielles créées
                    int potentialLines = countPotentialWinningLines(grid, localRow, localCol, mark);

                    // Si plus d'une ligne potentielle, c'est une opportunité de fourche
                    if (potentialLines >= 2) {
                        count++;
                    }

                    // Annuler le coup simulé
                    grid[row][col] = Mark.EMPTY;
                }
            }
        }

        return count;
    }

    // Compter les lignes potentielles (lignes avec 2 marques et une case vide)
    private int countPotentialWinningLines(Mark[][] grid, int localRow, int localCol, Mark mark) {
        int count = 0;

        // Vérifier les lignes
        for (int r = 0; r < 3; r++) {
            int markCount = 0, emptyCount = 0;
            for (int c = 0; c < 3; c++) {
                int row = localRow * 3 + r;
                int col = localCol * 3 + c;
                if (grid[row][col] == mark) markCount++;
                else if (grid[row][col] == Mark.EMPTY) emptyCount++;
            }
            if (markCount == 2 && emptyCount == 1) count++;
        }

        // Vérifier les colonnes
        for (int c = 0; c < 3; c++) {
            int markCount = 0, emptyCount = 0;
            for (int r = 0; r < 3; r++) {
                int row = localRow * 3 + r;
                int col = localCol * 3 + c;
                if (grid[row][col] == mark) markCount++;
                else if (grid[row][col] == Mark.EMPTY) emptyCount++;
            }
            if (markCount == 2 && emptyCount == 1) count++;
        }

        // Vérifier les diagonales
        int markCount1 = 0, emptyCount1 = 0;
        int markCount2 = 0, emptyCount2 = 0;

        for (int i = 0; i < 3; i++) {
            // Diagonale 1
            int row1 = localRow * 3 + i;
            int col1 = localCol * 3 + i;
            if (grid[row1][col1] == mark) markCount1++;
            else if (grid[row1][col1] == Mark.EMPTY) emptyCount1++;

            // Diagonale 2
            int row2 = localRow * 3 + i;
            int col2 = localCol * 3 + (2 - i);
            if (grid[row2][col2] == mark) markCount2++;
            else if (grid[row2][col2] == Mark.EMPTY) emptyCount2++;
        }

        if (markCount1 == 2 && emptyCount1 == 1) count++;
        if (markCount2 == 2 && emptyCount2 == 1) count++;

        return count;
    }

    // Vérification des coups gagnants
    public boolean isWinningMove(Board board, Move move, Mark mark) {
        int localRow = move.getGlobalRow();
        int localCol = move.getGlobalCol();

        // Créer une copie du plateau pour simuler le coup
        Board tempBoard = board.copy();
        tempBoard.play(move, mark);

        // Vérifier si ce coup gagne un plateau local
        Mark[][] localBoards = tempBoard.getLocalBoards();
        if (localBoards[localRow][localCol] == mark) {
            // Vérifier ensuite si ce plateau local complète une ligne globale
            Mark globalWinner = checkGlobalWinner(localBoards);
            return globalWinner == mark;
        }

        return false;
    }
}