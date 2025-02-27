/**
 * Enhanced evaluator for the Ultimate Tic-Tac-Toe game
 */
public class Evaluator {
    // Constants for evaluating the board
    private static final int WIN_SCORE = 10000;
    private static final int POTENTIAL_WIN_SCORE = 1000;
    private static final int TWO_IN_A_ROW_SCORE = 100;
    private static final int STRATEGIC_LOCAL_BOARD_SCORE = 500;

    // Value different positions in the local board
    private static final int CENTER_SCORE = 5;
    private static final int CORNER_SCORE = 3;
    private static final int EDGE_SCORE = 1;

    // Value different local boards on the global board
    private static final int CENTER_BOARD_BONUS = 3;
    private static final int CORNER_BOARD_BONUS = 2;
    private static final int EDGE_BOARD_BONUS = 1;

    // Board position weights matrix for local boards (higher values for strategic positions)
    private static final int[][] POSITION_WEIGHTS = {
            {3, 1, 3}, // Corner, Edge, Corner
            {1, 5, 1}, // Edge, Center, Edge
            {3, 1, 3}  // Corner, Edge, Corner
    };

    // Board weights matrix for global board (which local boards are more valuable)
    private static final int[][] BOARD_WEIGHTS = {
            {3, 2, 3}, // Corner, Edge, Corner boards
            {2, 4, 2}, // Edge, Center, Edge boards
            {3, 2, 3}  // Corner, Edge, Corner boards
    };

    // Evaluate the board position for the given player (4 for X, 2 for O)
    public static int evaluate(Board board, int player) {
        int opponent = (player == 4) ? 2 : 4;

        // Check if the game is over
        int gameStatus = board.checkGameStatus();
        if (gameStatus == player) {
            return WIN_SCORE;
        } else if (gameStatus == opponent) {
            return -WIN_SCORE;
        } else if (gameStatus == 1) {
            return 0; // Draw
        }

        int score = 0;
        int[][] boardState = board.getBoard();
        int[] localBoardStatus = board.getLocalBoardStatus();

        // Track threats and potential wins at the global level
        int playerLocalWins = 0;
        int opponentLocalWins = 0;

        // Evaluate each local board
        for (int boardRow = 0; boardRow < 3; boardRow++) {
            for (int boardCol = 0; boardCol < 3; boardCol++) {
                int localBoard = boardRow * 3 + boardCol;
                int boardWeight = BOARD_WEIGHTS[boardRow][boardCol];

                // Get starting position of this local board
                int startRow = boardRow * 3;
                int startCol = boardCol * 3;

                // If the local board is won, add appropriate points
                if (localBoardStatus[localBoard] == player) {
                    score += STRATEGIC_LOCAL_BOARD_SCORE * boardWeight;
                    playerLocalWins++;
                } else if (localBoardStatus[localBoard] == opponent) {
                    score -= STRATEGIC_LOCAL_BOARD_SCORE * boardWeight;
                    opponentLocalWins++;
                } else if (localBoardStatus[localBoard] == 0) {
                    // Evaluate open local board in more detail
                    score += evaluateLocalBoard(boardState, startRow, startCol, player, opponent, boardWeight);
                }
            }
        }

        // Evaluate global board patterns
        score += evaluateGlobalPatterns(localBoardStatus, player, opponent);

        // Evaluate forcing moves and strategic next board selection
        int nextLocalBoard = board.getNextLocalBoard();
        if (nextLocalBoard != -1) {
            // Where is the next board located (center, corner, edge)?
            int nextRow = nextLocalBoard / 3;
            int nextCol = nextLocalBoard % 3;
            int nextBoardWeight = BOARD_WEIGHTS[nextRow][nextCol];

            // If next board is already won or full, it's a free move for opponent (bad)
            if (localBoardStatus[nextLocalBoard] != 0) {
                score -= 150;
            }
            // If next board is strategic but not won by opponent, it's a disadvantage
            else if (nextBoardWeight > 1) {
                score -= nextBoardWeight * 50;
            }
        }

        // Evaluate global winning threats
        if (playerLocalWins >= 2) {
            score += playerLocalWins * 100;
        }

        if (opponentLocalWins >= 2) {
            score -= opponentLocalWins * 150; // Prioritize defense a bit more
        }

        return score;
    }

    // Detailed evaluation of an open local board
    private static int evaluateLocalBoard(int[][] boardState, int startRow, int startCol,
                                          int player, int opponent, int boardWeight) {
        int score = 0;

        // Count pieces and potential wins
        int playerCount = 0;
        int opponentCount = 0;

        // First count all pieces in this local board
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
                    score -= positionWeight * 4; // Penalize opponent pieces more
                }
            }
        }

        // Center control is particularly important in local boards
        if (boardState[startRow + 1][startCol + 1] == player) {
            score += CENTER_SCORE * 2 * boardWeight;
        } else if (boardState[startRow + 1][startCol + 1] == opponent) {
            score -= CENTER_SCORE * 3 * boardWeight;
        } else {
            // Empty center is valuable potential
            score += boardWeight;
        }

        // Evaluate rows, columns, and diagonals
        score += evaluateLocalLines(boardState, startRow, startCol, player, opponent, boardWeight);

        // Evaluate forks (multiple winning threats)
        score += evaluateForks(boardState, startRow, startCol, player, opponent);

        return score;
    }

    // Evaluate potential forks (multiple winning paths)
    private static int evaluateForks(int[][] boardState, int startRow, int startCol, int player, int opponent) {
        int score = 0;
        int playerWinningPaths = 0;
        int opponentWinningPaths = 0;

        // Check rows
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

            // If only player pieces and empty spots, it's a potential winning path
            if (playerPieces > 0 && opponentPieces == 0 && emptyCount > 0) {
                playerWinningPaths++;
            }

            // If only opponent pieces and empty spots, it's a potential losing path
            if (opponentPieces > 0 && playerPieces == 0 && emptyCount > 0) {
                opponentWinningPaths++;
            }
        }

        // Check columns
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

        // Check diagonal (top-left to bottom-right)
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

        // Check diagonal (top-right to bottom-left)
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

        // Multiple winning paths creates a fork situation
        if (playerWinningPaths >= 2) {
            score += playerWinningPaths * 50;
        }

        if (opponentWinningPaths >= 2) {
            score -= opponentWinningPaths * 60; // Penalize opponent forks more
        }

        return score;
    }

    // Evaluate rows, columns, and diagonals in a local board
    private static int evaluateLocalLines(int[][] boardState, int startRow, int startCol,
                                          int player, int opponent, int boardWeight) {
        int score = 0;

        // Evaluate rows
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

        // Evaluate columns
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

        // Evaluate diagonal (top-left to bottom-right)
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

        // Diagonals are slightly more valuable
        score += evaluateLine(playerCount, opponentCount, emptyCount, boardWeight) * 1.2;

        // Evaluate diagonal (top-right to bottom-left)
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

        // Diagonals are slightly more valuable
        score += evaluateLine(playerCount, opponentCount, emptyCount, boardWeight) * 1.2;

        return score;
    }

    // Evaluate global board patterns
    private static int evaluateGlobalPatterns(int[] localBoardStatus, int player, int opponent) {
        int score = 0;

        // Evaluate rows
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

        // Evaluate columns
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

        // Evaluate diagonal (top-left to bottom-right)
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

        // Diagonals are slightly more valuable at global level
        score += evaluateGlobalLine(playerCount, opponentCount, emptyCount) * 1.5;

        // Evaluate diagonal (top-right to bottom-left)
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

        // Diagonals are slightly more valuable at global level
        score += evaluateGlobalLine(playerCount, opponentCount, emptyCount) * 1.5;

        return score;
    }

    // Evaluate a line (row, column, or diagonal) in a local board
    private static int evaluateLine(int playerCount, int opponentCount, int emptyCount, int boardWeight) {
        int score = 0;

        // If only player's pieces in the line
        if (playerCount > 0 && opponentCount == 0) {
            if (playerCount == 1) {
                score += 1 * boardWeight;
            } else if (playerCount == 2 && emptyCount == 1) {
                score += TWO_IN_A_ROW_SCORE * boardWeight; // Two pieces with the third position empty
            }
        }

        // If only opponent's pieces in the line
        if (opponentCount > 0 && playerCount == 0) {
            if (opponentCount == 1) {
                score -= 1 * boardWeight;
            } else if (opponentCount == 2 && emptyCount == 1) {
                // More penalty for opponent about to win
                score -= TWO_IN_A_ROW_SCORE * 1.5 * boardWeight;
            }
        }

        return score;
    }

    // Evaluate a line at the global board level
    private static int evaluateGlobalLine(int playerCount, int opponentCount, int emptyCount) {
        int score = 0;

        // If only player's wins in the line
        if (playerCount > 0 && opponentCount == 0) {
            if (playerCount == 1 && emptyCount == 2) {
                score += 75; // One win with two potential boards
            } else if (playerCount == 2 && emptyCount == 1) {
                score += POTENTIAL_WIN_SCORE; // Two wins with one more needed to win the game
            }
        }

        // If only opponent's wins in the line
        if (opponentCount > 0 && playerCount == 0) {
            if (opponentCount == 1 && emptyCount == 2) {
                score -= 100; // Block opponent's progress harder
            } else if (opponentCount == 2 && emptyCount == 1) {
                score -= POTENTIAL_WIN_SCORE * 1.2; // Block opponent's win with urgency
            }
        }

        // Mixed lines with both player and opponent wins have no clear advantage
        if (playerCount > 0 && opponentCount > 0) {
            // The line is blocked, slightly negative because it limits options
            score -= 5;
        }

        return score;
    }
}