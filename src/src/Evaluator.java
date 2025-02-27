/**
 * Évaluateur pour jeu Ultimate Tic-Tac-Toe
 */
public class Evaluator {
    // Constantes pour l'évaluation
    private static final int WIN_SCORE = 10000;
    private static final int POTENTIAL_WIN_SCORE = 1000;
    private static final int TWO_IN_A_ROW_SCORE = 100;
    private static final int STRATEGIC_LOCAL_BOARD_SCORE = 500;

    // Valeurs des positions dans plateau local
    private static final int CENTER_SCORE = 5;
    private static final int CORNER_SCORE = 3;
    private static final int EDGE_SCORE = 1;

    // Valeurs des plateaux locaux
    private static final int CENTER_BOARD_BONUS = 3;
    private static final int CORNER_BOARD_BONUS = 2;
    private static final int EDGE_BOARD_BONUS = 1;

    // Poids des positions pour plateaux locaux
    private static final int[][] POSITION_WEIGHTS = {
            {3, 1, 3}, // Coin, Bord, Coin
            {1, 5, 1}, // Bord, Centre, Bord
            {3, 1, 3}  // Coin, Bord, Coin
    };

    // Poids des plateaux locaux
    private static final int[][] BOARD_WEIGHTS = {
            {3, 2, 3}, // Plateaux Coin, Bord, Coin
            {2, 4, 2}, // Plateaux Bord, Centre, Bord
            {3, 2, 3}  // Plateaux Coin, Bord, Coin
    };

    // Évalue la position pour le joueur (4 pour X, 2 pour O)
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

        // Menaces et victoires potentielles
        int playerLocalWins = 0;
        int opponentLocalWins = 0;

        // Évalue chaque plateau local
        for (int boardRow = 0; boardRow < 3; boardRow++) {
            for (int boardCol = 0; boardCol < 3; boardCol++) {
                int localBoard = boardRow * 3 + boardCol;
                int boardWeight = BOARD_WEIGHTS[boardRow][boardCol];

                // Position de départ de ce plateau local
                int startRow = boardRow * 3;
                int startCol = boardCol * 3;

                // Si le plateau local est gagné
                if (localBoardStatus[localBoard] == player) {
                    score += STRATEGIC_LOCAL_BOARD_SCORE * boardWeight;
                    playerLocalWins++;
                } else if (localBoardStatus[localBoard] == opponent) {
                    score -= STRATEGIC_LOCAL_BOARD_SCORE * boardWeight;
                    opponentLocalWins++;
                } else if (localBoardStatus[localBoard] == 0) {
                    // Évalue un plateau local ouvert
                    score += evaluateLocalBoard(boardState, startRow, startCol, player, opponent, boardWeight);
                }
            }
        }

        // Évalue les motifs globaux
        score += evaluateGlobalPatterns(localBoardStatus, player, opponent);

        // Évalue les coups forcés et la sélection stratégique du plateau
        int nextLocalBoard = board.getNextLocalBoard();
        if (nextLocalBoard != -1) {
            // Où se trouve le prochain plateau ?
            int nextRow = nextLocalBoard / 3;
            int nextCol = nextLocalBoard % 3;
            int nextBoardWeight = BOARD_WEIGHTS[nextRow][nextCol];

            // Si prochain plateau est déjà gagné, c'est mauvais
            if (localBoardStatus[nextLocalBoard] != 0) {
                score -= 150;
            }
            // Si le prochain plateau est stratégique mais pas gagné, c'est désavantageux
            else if (nextBoardWeight > 1) {
                score -= nextBoardWeight * 50;
            }
        }

        // Évalue les menaces de victoire globales
        if (playerLocalWins >= 2) {
            score += playerLocalWins * 100;
        }

        if (opponentLocalWins >= 2) {
            score -= opponentLocalWins * 150; // Priorité à la défense
        }

        return score;
    }

    // Évaluation détaillée d'un plateau local ouvert
    private static int evaluateLocalBoard(int[][] boardState, int startRow, int startCol,
                                          int player, int opponent, int boardWeight) {
        int score = 0;

        // Compte pièces et victoires potentielles
        int playerCount = 0;
        int opponentCount = 0;

        // Compte pièces dans ce plateau local
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int row = startRow + i;
                int col = startCol + j;
                int cellValue = boardState[row][col];
                int positionWeight = POSITION_WEIGHTS[i][j];

                if (cellValue == player) {
                    playerCount++;
                    score += positionWeight * 3;
                } else if (cellValue == opponent) {
                    opponentCount++;
                    score -= positionWeight * 4;
                }
            }
        }

        // Le contrôle du centre est important
        if (boardState[startRow + 1][startCol + 1] == player) {
            score += CENTER_SCORE * 2 * boardWeight;
        } else if (boardState[startRow + 1][startCol + 1] == opponent) {
            score -= CENTER_SCORE * 3 * boardWeight;
        } else {
            // Centre vide est un potentiel
            score += boardWeight;
        }

        // Évalue lignes, colonnes et diagonales
        score += evaluateLocalLines(boardState, startRow, startCol, player, opponent, boardWeight);

        // Évalue les fourchettes
        score += evaluateForks(boardState, startRow, startCol, player, opponent);

        return score;
    }

    // Évalue les fourchettes
    private static int evaluateForks(int[][] boardState, int startRow, int startCol, int player, int opponent) {
        int score = 0;
        int playerWinningPaths = 0;
        int opponentWinningPaths = 0;

        // Vérifie les lignes
        for (int i = 0; i < 3; i++) {
            int playerPieces = 0;
            int opponentPieces = 0;
            int emptyCount = 0;

            for (int j = 0; j < 3; j++) {
                int cell = boardState[startRow + i][startCol + j];
                if (cell == player) playerPieces++;
                else if (cell == opponent) opponentPieces++;
                else emptyCount++;
            }

            // Ligne avec pièces du joueur et cases vides
            if (playerPieces > 0 && opponentPieces == 0 && emptyCount > 0) {
                playerWinningPaths++;
            }

            // Ligne avec pièces adverses et cases vides
            if (opponentPieces > 0 && playerPieces == 0 && emptyCount > 0) {
                opponentWinningPaths++;
            }
        }

        // Vérifie les colonnes
        for (int j = 0; j < 3; j++) {
            int playerPieces = 0;
            int opponentPieces = 0;
            int emptyCount = 0;

            for (int i = 0; i < 3; i++) {
                int cell = boardState[startRow + i][startCol + j];
                if (cell == player) playerPieces++;
                else if (cell == opponent) opponentPieces++;
                else emptyCount++;
            }

            if (playerPieces > 0 && opponentPieces == 0 && emptyCount > 0) {
                playerWinningPaths++;
            }

            if (opponentPieces > 0 && playerPieces == 0 && emptyCount > 0) {
                opponentWinningPaths++;
            }
        }

        // Diagonale (haut-gauche vers bas-droite)
        int playerPieces = 0;
        int opponentPieces = 0;
        int emptyCount = 0;

        for (int i = 0; i < 3; i++) {
            int cell = boardState[startRow + i][startCol + i];
            if (cell == player) playerPieces++;
            else if (cell == opponent) opponentPieces++;
            else emptyCount++;
        }

        if (playerPieces > 0 && opponentPieces == 0 && emptyCount > 0) {
            playerWinningPaths++;
        }

        if (opponentPieces > 0 && playerPieces == 0 && emptyCount > 0) {
            opponentWinningPaths++;
        }

        // Diagonale (haut-droite vers bas-gauche)
        playerPieces = 0;
        opponentPieces = 0;
        emptyCount = 0;

        for (int i = 0; i < 3; i++) {
            int cell = boardState[startRow + i][startCol + 2 - i];
            if (cell == player) playerPieces++;
            else if (cell == opponent) opponentPieces++;
            else emptyCount++;
        }

        if (playerPieces > 0 && opponentPieces == 0 && emptyCount > 0) {
            playerWinningPaths++;
        }

        if (opponentPieces > 0 && playerPieces == 0 && emptyCount > 0) {
            opponentWinningPaths++;
        }

        // Plusieurs chemins de victoire créent une fourchette
        if (playerWinningPaths >= 2) {
            score += playerWinningPaths * 50;
        }

        if (opponentWinningPaths >= 2) {
            score -= opponentWinningPaths * 60; // Pénaliser davantage les fourchettes adverses
        }

        return score;
    }

    // Évalue lignes, colonnes et diagonales d'un plateau local
    private static int evaluateLocalLines(int[][] boardState, int startRow, int startCol,
                                          int player, int opponent, int boardWeight) {
        int score = 0;

        // Évalue les lignes
        for (int i = 0; i < 3; i++) {
            int playerCount = 0;
            int opponentCount = 0;
            int emptyCount = 0;

            for (int j = 0; j < 3; j++) {
                int cell = boardState[startRow + i][startCol + j];
                if (cell == player) {
                    playerCount++;
                } else if (cell == opponent) {
                    opponentCount++;
                } else {
                    emptyCount++;
                }
            }

            score += evaluateLine(playerCount, opponentCount, emptyCount, boardWeight);
        }

        // Évalue les colonnes
        for (int j = 0; j < 3; j++) {
            int playerCount = 0;
            int opponentCount = 0;
            int emptyCount = 0;

            for (int i = 0; i < 3; i++) {
                int cell = boardState[startRow + i][startCol + j];
                if (cell == player) {
                    playerCount++;
                } else if (cell == opponent) {
                    opponentCount++;
                } else {
                    emptyCount++;
                }
            }

            score += evaluateLine(playerCount, opponentCount, emptyCount, boardWeight);
        }

        // Diagonale (haut-gauche vers bas-droite)
        int playerCount = 0;
        int opponentCount = 0;
        int emptyCount = 0;

        for (int i = 0; i < 3; i++) {
            int cell = boardState[startRow + i][startCol + i];
            if (cell == player) {
                playerCount++;
            } else if (cell == opponent) {
                opponentCount++;
            } else {
                emptyCount++;
            }
        }

        // Diagonales légèrement plus valorisées
        score += evaluateLine(playerCount, opponentCount, emptyCount, boardWeight) * 1.2;

        // Diagonale (haut-droite vers bas-gauche)
        playerCount = 0;
        opponentCount = 0;
        emptyCount = 0;

        for (int i = 0; i < 3; i++) {
            int cell = boardState[startRow + i][startCol + 2 - i];
            if (cell == player) {
                playerCount++;
            } else if (cell == opponent) {
                opponentCount++;
            } else {
                emptyCount++;
            }
        }

        // Diagonales légèrement plus valorisées
        score += evaluateLine(playerCount, opponentCount, emptyCount, boardWeight) * 1.2;

        return score;
    }

    // Évalue motifs globaux
    private static int evaluateGlobalPatterns(int[] localBoardStatus, int player, int opponent) {
        int score = 0;

        // Évalue lignes
        for (int i = 0; i < 3; i++) {
            int playerCount = 0;
            int opponentCount = 0;
            int emptyCount = 0;

            for (int j = 0; j < 3; j++) {
                int localBoard = i * 3 + j;
                if (localBoardStatus[localBoard] == player) {
                    playerCount++;
                } else if (localBoardStatus[localBoard] == opponent) {
                    opponentCount++;
                } else if (localBoardStatus[localBoard] == 0) {
                    emptyCount++;
                }
            }

            score += evaluateGlobalLine(playerCount, opponentCount, emptyCount);
        }

        // Évalue colonnes
        for (int j = 0; j < 3; j++) {
            int playerCount = 0;
            int opponentCount = 0;
            int emptyCount = 0;

            for (int i = 0; i < 3; i++) {
                int localBoard = i * 3 + j;
                if (localBoardStatus[localBoard] == player) {
                    playerCount++;
                } else if (localBoardStatus[localBoard] == opponent) {
                    opponentCount++;
                } else if (localBoardStatus[localBoard] == 0) {
                    emptyCount++;
                }
            }

            score += evaluateGlobalLine(playerCount, opponentCount, emptyCount);
        }

        // Diagonale (haut-gauche vers bas-droite)
        int playerCount = 0;
        int opponentCount = 0;
        int emptyCount = 0;

        for (int i = 0; i < 3; i++) {
            int localBoard = i * 3 + i;
            if (localBoardStatus[localBoard] == player) {
                playerCount++;
            } else if (localBoardStatus[localBoard] == opponent) {
                opponentCount++;
            } else if (localBoardStatus[localBoard] == 0) {
                emptyCount++;
            }
        }

        // Diagonales plus valorisées au niveau global
        score += evaluateGlobalLine(playerCount, opponentCount, emptyCount) * 1.5;

        // Diagonale (haut-droite vers bas-gauche)
        playerCount = 0;
        opponentCount = 0;
        emptyCount = 0;

        for (int i = 0; i < 3; i++) {
            int localBoard = i * 3 + (2 - i);
            if (localBoardStatus[localBoard] == player) {
                playerCount++;
            } else if (localBoardStatus[localBoard] == opponent) {
                opponentCount++;
            } else if (localBoardStatus[localBoard] == 0) {
                emptyCount++;
            }
        }

        // Diagonales plus valorisées au niveau global
        score += evaluateGlobalLine(playerCount, opponentCount, emptyCount) * 1.5;

        return score;
    }

    // Évalue une ligne dans un plateau local
    private static int evaluateLine(int playerCount, int opponentCount, int emptyCount, int boardWeight) {
        int score = 0;

        // Si seulement pièces du joueur dans la ligne
        if (playerCount > 0 && opponentCount == 0) {
            if (playerCount == 1) {
                score += 1 * boardWeight;
            } else if (playerCount == 2 && emptyCount == 1) {
                score += TWO_IN_A_ROW_SCORE * boardWeight; // Deux pièces avec la troisième vide
            }
        }

        // Si seulement pièces de l'adversaire
        if (opponentCount > 0 && playerCount == 0) {
            if (opponentCount == 1) {
                score -= 1 * boardWeight;
            } else if (opponentCount == 2 && emptyCount == 1) {
                // Pénalité pour adversaire sur le point de gagner
                score -= TWO_IN_A_ROW_SCORE * 1.5 * boardWeight;
            }
        }

        return score;
    }

    // Évalue une ligne au niveau global
    private static int evaluateGlobalLine(int playerCount, int opponentCount, int emptyCount) {
        int score = 0;

        // Si seulement des victoires du joueur dans la ligne
        if (playerCount > 0 && opponentCount == 0) {
            if (playerCount == 1 && emptyCount == 2) {
                score += 75; // Une victoire avec deux plateaux potentiels
            } else if (playerCount == 2 && emptyCount == 1) {
                score += POTENTIAL_WIN_SCORE; // Deux victoires avec une de plus pour gagner
            }
        }

        // Si seulement des victoires de l'adversaire
        if (opponentCount > 0 && playerCount == 0) {
            if (opponentCount == 1 && emptyCount == 2) {
                score -= 100; // Bloquer la progression adverse
            } else if (opponentCount == 2 && emptyCount == 1) {
                score -= POTENTIAL_WIN_SCORE * 1.2; // Bloquer la victoire adverse
            }
        }

        // Lignes mixtes avec victoires des deux joueurs
        if (playerCount > 0 && opponentCount > 0) {
            // Ligne bloquée, légèrement négatif
            score -= 5;
        }

        return score;
    }
}