class BoardEvaluator {
    private final Mark cpuMark;
    private final Mark humanMark;

    // Evaluation constants (similar to JavaScript implementation)
    private static final double[] POSITION_SCORES = {0.3, 0.2, 0.3, 0.2, 0.4, 0.2, 0.3, 0.2, 0.3};
    private static final double[] LOCAL_BOARD_WEIGHTINGS = {1.35, 1.0, 1.35, 1.0, 1.7, 1.0, 1.35, 1.0, 1.35};
    private static final int GLOBAL_WIN_VALUE = 50000;
    private static final int LOCAL_BOARD_WIN_VALUE = 150;

    // Winning combinations
    private static final int[][] WINNING_COMBOS = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},  // Rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},  // Columns
            {0, 4, 8}, {2, 4, 6}              // Diagonals
    };

    public BoardEvaluator(Mark cpuMark) {
        this.cpuMark = cpuMark;
        this.humanMark = (cpuMark == Mark.X) ? Mark.O : Mark.X;
    }

    public int evaluate(Board board, int lastPlayedGlobalIndex) {
        int score = 0;
        Mark[] globalBoard = board.getGlobalBoard();
        Mark[][] localBoards = board.getLocalBoards();

        // Global board win check
        Mark globalWinner = board.checkGlobalWinner();
        if (globalWinner == humanMark) {
            score += GLOBAL_WIN_VALUE;
        } else if (globalWinner == cpuMark) {
            score -= GLOBAL_WIN_VALUE;
        }

        // Evaluate position value for each local board
        for (int i = 0; i < 9; i++) {
            if (globalBoard[i] == humanMark) {
                score += POSITION_SCORES[i] * LOCAL_BOARD_WIN_VALUE;
            } else if (globalBoard[i] == cpuMark) {
                score -= POSITION_SCORES[i] * LOCAL_BOARD_WIN_VALUE;
            } else if (globalBoard[i] == Mark.EMPTY) {
                // Evaluate individual positions in this board
                for (int j = 0; j < 9; j++) {
                    if (localBoards[i][j] == humanMark) {
                        double positionValue = POSITION_SCORES[j] * LOCAL_BOARD_WEIGHTINGS[i];
                        // If this is the last played board, increase importance
                        if (i == lastPlayedGlobalIndex) {
                            positionValue *= 1.5;
                        }
                        score += positionValue;
                    } else if (localBoards[i][j] == cpuMark) {
                        double positionValue = POSITION_SCORES[j] * LOCAL_BOARD_WEIGHTINGS[i];
                        // If this is the last played board, increase importance
                        if (i == lastPlayedGlobalIndex) {
                            positionValue *= 1.5;
                        }
                        score -= positionValue;
                    }
                }

                // Evaluate winning threats in local boards
                evaluateLocalBoardLines(localBoards[i], i, lastPlayedGlobalIndex, score);
            }
        }

        // Evaluate global board lines
        evaluateGlobalBoardLines(globalBoard, score);

        return score;
    }

    private void evaluateLocalBoardLines(Mark[] localBoard, int boardIndex, int lastPlayedGlobalIndex, int score) {
        // Track scores we've already counted to avoid duplicates
        java.util.Set<Integer> countedScores = new java.util.HashSet<>();

        for (int[] combo : WINNING_COMBOS) {
            Mark[] line = new Mark[]{localBoard[combo[0]], localBoard[combo[1]], localBoard[combo[2]]};
            int lineScore = scoreRow(line);

            if (lineScore != 0 && !countedScores.contains(lineScore)) {
                // Extra weight for diagonals
                boolean isDiagonal = (combo[0] == 0 && combo[1] == 4 && combo[2] == 8) ||
                        (combo[0] == 2 && combo[1] == 4 && combo[2] == 6);

                double multiplier = 1.0;
                if (isDiagonal && (lineScore == 6 || lineScore == -6)) {
                    multiplier *= 1.2;
                }

                // Extra weight for the board that was just played in
                if (boardIndex == lastPlayedGlobalIndex) {
                    multiplier *= 1.5;
                }

                score += lineScore * multiplier * LOCAL_BOARD_WEIGHTINGS[boardIndex];
                countedScores.add(lineScore);
            }
        }
    }

    private void evaluateGlobalBoardLines(Mark[] globalBoard, int score) {
        // Track scores we've already counted to avoid duplicates
        java.util.Set<Integer> countedScores = new java.util.HashSet<>();

        for (int[] combo : WINNING_COMBOS) {
            Mark[] line = new Mark[]{globalBoard[combo[0]], globalBoard[combo[1]], globalBoard[combo[2]]};
            int lineScore = scoreRow(line);

            if (lineScore != 0 && !countedScores.contains(lineScore)) {
                // Extra weight for diagonals
                boolean isDiagonal = (combo[0] == 0 && combo[1] == 4 && combo[2] == 8) ||
                        (combo[0] == 2 && combo[1] == 4 && combo[2] == 6);

                double multiplier = 1.0;
                if (isDiagonal && (lineScore == 6 || lineScore == -6)) {
                    multiplier *= 1.2;
                }

                score += lineScore * multiplier * LOCAL_BOARD_WIN_VALUE;
                countedScores.add(lineScore);
            }
        }
    }

    private int scoreRow(Mark[] row) {
        int oCount = 0;
        int xCount = 0;
        int emptyCount = 0;

        for (Mark mark : row) {
            if (mark == Mark.O) {
                oCount++;
            } else if (mark == Mark.X) {
                xCount++;
            } else if (mark == Mark.EMPTY) {
                emptyCount++;
            }
        }

        // Return scores similar to JavaScript implementation
        if (oCount == 3) return -12;
        if (oCount == 2 && emptyCount == 1) return -6;
        if (xCount == 2 && emptyCount == 1) return 6;
        if (xCount == 2 && oCount == 1) return -9;
        if (xCount == 3) return 12;
        if (oCount == 2 && xCount == 1) return 9;

        return 0;
    }
}