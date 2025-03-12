/**
 * Évaluateur pour jeu Ultimate Tic-Tac-Toe
 */
public class Evaluator {
    // Constantes pour l'évaluation
    private static final int WIN_SCORE = 10000;
    private static final int POTENTIAL_WIN_SCORE = 1000;
    private static final int TWO_IN_A_ROW_SCORE = 100;
    private static final int STRATEGIC_LOCAL_BOARD_SCORE = 500;
    private static final int CENTER_SCORE = 25;

    // Poids des positions pour plateaux locaux
    private static final int[][] POSITION_WEIGHTS = {
            {3, 1, 3},
            {1, 5, 1},
            {3, 1, 3}
    };

    // Poids des plateaux locaux
    private static final int[][] BOARD_WEIGHTS = {
            {3, 2, 3},
            {2, 5, 2},
            {3, 2, 3}
    };

    // Directions pour analyser les lignes (horizontal, vertical, diagonal)
    private static final int[][][] DIRECTIONS = {
            {{0, 0}, {0, 1}, {0, 2}},  // Lignes
            {{0, 0}, {1, 0}, {2, 0}},  // Colonnes
            {{0, 0}, {1, 1}, {2, 2}},  // Diagonale principale
            {{0, 2}, {1, 1}, {2, 0}}   // Diagonale secondaire
    };

    /**
     * Évalue la position pour le joueur (4 pour X, 2 pour O)
     */
    public static int evaluate(Board board, int player) {
        int opponent = (player == 4) ? 2 : 4;

        // Vérifie si la partie est terminée
        int gameStatus = board.checkGameStatus();
        if (gameStatus == player) {
            return WIN_SCORE;
        } else if (gameStatus == opponent) {
            return -WIN_SCORE;
        } else if (gameStatus == 1) {
            return 0; // Match nul
        }

        int score = 0;
        int[][] boardState = board.getBoard();
        int[] localBoardStatus = board.getLocalBoardStatus();

        // Compteurs de plateaux gagnés
        int playerLocalWins = 0;
        int opponentLocalWins = 0;

        // Évalue chaque plateau local
        for (int boardRow = 0; boardRow < 3; boardRow++) {
            for (int boardCol = 0; boardCol < 3; boardCol++) {
                int localBoard = boardRow * 3 + boardCol;
                int boardWeight = BOARD_WEIGHTS[boardRow][boardCol];
                int startRow = boardRow * 3;
                int startCol = boardCol * 3;

                // Traitement selon le statut du plateau local
                if (localBoardStatus[localBoard] == player) {
                    score += STRATEGIC_LOCAL_BOARD_SCORE * boardWeight;
                    playerLocalWins++;
                } else if (localBoardStatus[localBoard] == opponent) {
                    score -= STRATEGIC_LOCAL_BOARD_SCORE * boardWeight;
                    opponentLocalWins++;
                } else if (localBoardStatus[localBoard] == 0) { // Évalue la position si le plateau n'est pas fermé
                    score += evaluateLocalBoard(boardState, startRow, startCol, player, opponent, boardWeight);
                }
            }
        }

        // Évalue les motifs globaux
        score += evaluateGlobalPatterns(localBoardStatus, player, opponent);

        // Évalue les coups forcés (vers ou est envoyé l'adversaire)
        int nextLocalBoard = board.getNextLocalBoard();
        if (nextLocalBoard != -1) {
            int nextRow = nextLocalBoard / 3;
            int nextCol = nextLocalBoard % 3;
            int nextBoardWeight = BOARD_WEIGHTS[nextRow][nextCol];

            // Pénalité si prochain plateau déjà gagné ou stratégique
            if (localBoardStatus[nextLocalBoard] != 0) {
                score -= 200;
            } else if (nextBoardWeight > 1) {
                score -= nextBoardWeight * 50;
            }
        }

        // Bonus/pénalité pour contrôle de multiples plateaux
        if (playerLocalWins >= 2) {
            score += playerLocalWins * 100;
        }
        if (opponentLocalWins >= 2) {
            score -= opponentLocalWins * 150;
        }

        return score;
    }

    /**
     * Évalue un plateau local ouvert
     */
    private static int evaluateLocalBoard(int[][] boardState, int startRow, int startCol,
                                          int player, int opponent, int boardWeight) {
        int score = 0;

        // Évalue les positions occupées
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int row = startRow + i;
                int col = startCol + j;
                int cellValue = boardState[row][col];
                int positionWeight = POSITION_WEIGHTS[i][j];

                if (cellValue == player) {
                    score += positionWeight * 3;
                } else if (cellValue == opponent) {
                    score -= positionWeight * 4;
                }
            }
        }

        // Bonus spécial pour le centre
        int centerValue = boardState[startRow + 1][startCol + 1];
        if (centerValue == player) {
            score += CENTER_SCORE * 2 * boardWeight;
        } else if (centerValue == opponent) {
            score -= CENTER_SCORE * 3 * boardWeight;
        } else {
            score += boardWeight; // Centre vide = potentiel
        }

        // Évalue les alignements et fourchettes
        score += evaluateLocalLines(boardState, startRow, startCol, player, opponent, boardWeight);
        score += evaluateForks(boardState, startRow, startCol, player, opponent);

        return score;
    }

    /**
     * Évalue les alignements dans un plateau local
     */
    private static int evaluateLocalLines(int[][] boardState, int startRow, int startCol,
                                          int player, int opponent, int boardWeight) {
        int score = 0;

        // Analyse toutes les directions (lignes, colonnes, diagonales)
        for (int dir = 0; dir < DIRECTIONS.length; dir++) {
            int playerCount = 0;
            int opponentCount = 0;
            int emptyCount = 0;

            // Compte les pièces dans cette direction
            for (int i = 0; i < 3; i++) {
                int row = startRow + DIRECTIONS[dir][i][0];
                int col = startCol + DIRECTIONS[dir][i][1];
                int cell = boardState[row][col];

                if (cell == player) playerCount++;
                else if (cell == opponent) opponentCount++;
                else emptyCount++;
            }

            // Calcule le score pour cette ligne
            int lineScore = evaluateLine(playerCount, opponentCount, emptyCount, boardWeight);

            // Bonus pour les diagonales
            if (dir >= 2) {  // Si c'est une diagonale
                lineScore = (lineScore * 12) / 10;  // Équivalent à * 1.2
            }

            score += lineScore;
        }

        return score;
    }

    /**
     * Évalue les fourchettes (menaces multiples) dans un plateau local
     */
    private static int evaluateForks(int[][] boardState, int startRow, int startCol, int player, int opponent) {
        int playerWinningPaths = 0;
        int opponentWinningPaths = 0;

        // Analyse toutes les directions
        for (int dir = 0; dir < DIRECTIONS.length; dir++) {
            int playerPieces = 0;
            int opponentPieces = 0;
            int emptyCount = 0;

            // Compte les pièces dans cette direction
            for (int i = 0; i < 3; i++) {
                int row = startRow + DIRECTIONS[dir][i][0];
                int col = startCol + DIRECTIONS[dir][i][1];
                int cell = boardState[row][col];

                if (cell == player) playerPieces++;
                else if (cell == opponent) opponentPieces++;
                else emptyCount++;
            }

            // Vérifie les chemins de victoire potentiels
            if (playerPieces > 0 && opponentPieces == 0 && emptyCount > 0) {
                playerWinningPaths++;
            }
            if (opponentPieces > 0 && playerPieces == 0 && emptyCount > 0) {
                opponentWinningPaths++;
            }
        }

        // Calcule le score des fourchettes
        int score = 0;
        if (playerWinningPaths >= 2) {
            score += playerWinningPaths * 50;
        }
        if (opponentWinningPaths >= 2) {
            score -= opponentWinningPaths * 60;
        }

        return score;
    }

    /**
     * Évalue les motifs globaux (alignements de plateaux)
     */
    private static int evaluateGlobalPatterns(int[] localBoardStatus, int player, int opponent) {
        int score = 0;

        // Pour chaque direction possible (lignes, colonnes, diagonales)
        for (int pattern = 0; pattern < 8; pattern++) {
            int playerCount = 0;
            int opponentCount = 0;
            int emptyCount = 0;
            int[] positions = getGlobalPositions(pattern);

            // Compte les plateaux gagnés dans cette direction
            for (int pos : positions) {
                if (localBoardStatus[pos] == player) {
                    playerCount++;
                } else if (localBoardStatus[pos] == opponent) {
                    opponentCount++;
                } else if (localBoardStatus[pos] == 0) {
                    emptyCount++;
                }
            }

            // Calcule le score pour ce motif global
            int patternScore = evaluateGlobalLine(playerCount, opponentCount, emptyCount);

            // Bonus pour les diagonales
            if (pattern >= 6) {  // Diagonales
                patternScore = (patternScore * 15) / 10;  // Équivalent à * 1.5
            }

            score += patternScore;
        }

        return score;
    }

    /**
     * Retourne les positions à vérifier pour un motif global spécifique
     */
    private static int[] getGlobalPositions(int pattern) {
        switch (pattern) {
            case 0: return new int[] {0, 1, 2}; // Ligne 0
            case 1: return new int[] {3, 4, 5}; // Ligne 1
            case 2: return new int[] {6, 7, 8}; // Ligne 2
            case 3: return new int[] {0, 3, 6}; // Colonne 0
            case 4: return new int[] {1, 4, 7}; // Colonne 1
            case 5: return new int[] {2, 5, 8}; // Colonne 2
            case 6: return new int[] {0, 4, 8}; // Diagonale principale
            case 7: return new int[] {2, 4, 6}; // Diagonale secondaire
            default: return new int[0];
        }
    }

    /**
     * Évalue une ligne dans un plateau local
     */
    private static int evaluateLine(int playerCount, int opponentCount, int emptyCount, int boardWeight) {
        int score = 0;

        // Si seulement pièces du joueur dans la ligne
        if (playerCount > 0 && opponentCount == 0) {
            if (playerCount == 1) {
                score += boardWeight;
            } else if (playerCount == 2 && emptyCount == 1) {
                score += TWO_IN_A_ROW_SCORE * boardWeight;
            }
        }

        // Si seulement pièces de l'adversaire
        if (opponentCount > 0 && playerCount == 0) {
            if (opponentCount == 1) {
                score -= boardWeight;
            } else if (opponentCount == 2 && emptyCount == 1) {
                score -= (TWO_IN_A_ROW_SCORE * 15 * boardWeight) / 10; // * 1.5
            }
        }

        return score;
    }

    /**
     * Évalue une ligne au niveau global
     */
    private static int evaluateGlobalLine(int playerCount, int opponentCount, int emptyCount) {
        int score = 0;

        // Si seulement des victoires du joueur dans la ligne
        if (playerCount > 0 && opponentCount == 0) {
            if (playerCount == 1 && emptyCount == 2) {
                score += 75;
            } else if (playerCount == 2 && emptyCount == 1) {
                score += POTENTIAL_WIN_SCORE;
            }
        }

        // Si seulement des victoires de l'adversaire
        if (opponentCount > 0 && playerCount == 0) {
            if (opponentCount == 1 && emptyCount == 2) {
                score -= 100;
            } else if (opponentCount == 2 && emptyCount == 1) {
                score -= (POTENTIAL_WIN_SCORE * 12) / 10; // * 1.2
            }
        }

        // Lignes mixtes avec victoires des deux joueurs
        if (playerCount > 0 && opponentCount > 0) {
            score -= 5; // Ligne bloquée
        }

        return score;
    }
}