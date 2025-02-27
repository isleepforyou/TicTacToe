/**
 * Represents the Ultimate Tic-Tac-Toe game board
 */
public class Board {
    // The board is represented as a 9x9 grid, where each cell can be 0 (empty), 2 (O), or 4 (X)
    private int[][] board;

    // Keeps track of which local board the next player must play in (-1 if they can play anywhere)
    private int nextLocalBoard;

    // Keeps track of which local boards have been won by X, O, or are full (drawn)
    private int[] localBoardStatus; // 0: open, 4: X won, 2: O won, 1: drawn

    public Board() {
        board = new int[9][9];
        localBoardStatus = new int[9];
        nextLocalBoard = -1; // Initially, the player can play anywhere
    }

    // Constructor that creates a board from a given state
    public Board(int[][] board, int nextLocalBoard) {
        this.board = new int[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                this.board[i][j] = board[i][j];
            }
        }

        this.nextLocalBoard = nextLocalBoard;
        this.localBoardStatus = new int[9];
        updateLocalBoardStatuses();
    }

    // Copy constructor
    public Board(Board other) {
        this.board = new int[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                this.board[i][j] = other.board[i][j];
            }
        }

        this.nextLocalBoard = other.nextLocalBoard;
        this.localBoardStatus = new int[9];
        for (int i = 0; i < 9; i++) {
            this.localBoardStatus[i] = other.localBoardStatus[i];
        }
    }

    // Initialize the board from a 1D array (as received from the server)
    public void initializeBoard(int[] boardValues) {
        int x = 0, y = 0;
        for (int i = 0; i < boardValues.length; i++) {
            board[x][y] = boardValues[i];
            x++;
            if (x == 9) {
                x = 0;
                y++;
            }
        }
        updateLocalBoardStatuses();
    }

    // Make a move on the board
    public boolean makeMove(int globalRow, int globalCol, int player) {
        // Check if the move is valid
        if (!isValidMove(globalRow, globalCol)) {
            return false;
        }

        // Make the move
        board[globalRow][globalCol] = player;

        // Update the local board statuses
        updateLocalBoardStatuses();

        // Update the next local board (where the opponent must play)
        int localRow = globalRow % 3;
        int localCol = globalCol % 3;
        int nextBoard = localRow * 3 + localCol;

        // If the next board is already won or full, the opponent can play anywhere
        if (localBoardStatus[nextBoard] != 0) {
            nextLocalBoard = -1;
        } else {
            nextLocalBoard = nextBoard;
        }

        return true;
    }

    // Check if a move is valid
    public boolean isValidMove(int globalRow, int globalCol) {
        // Check if the cell is empty
        if (board[globalRow][globalCol] != 0) {
            return false;
        }

        // Check if the move is in the correct local board
        if (nextLocalBoard != -1) {
            int localBoardRow = globalRow / 3;
            int localBoardCol = globalCol / 3;
            int localBoard = localBoardRow * 3 + localBoardCol;

            if (localBoard != nextLocalBoard) {
                return false;
            }
        }

        // Check if the local board is already won or full
        int localBoardRow = globalRow / 3;
        int localBoardCol = globalCol / 3;
        int localBoard = localBoardRow * 3 + localBoardCol;

        return localBoardStatus[localBoard] == 0;
    }

    // Update the status of all local boards
    private void updateLocalBoardStatuses() {
        for (int i = 0; i < 9; i++) {
            int row = (i / 3) * 3;
            int col = (i % 3) * 3;

            localBoardStatus[i] = checkLocalBoardStatus(row, col);
        }
    }

    // Check the status of a local board (0: open, 4: X won, 2: O won, 1: drawn)
    private int checkLocalBoardStatus(int startRow, int startCol) {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[startRow + i][startCol] != 0 &&
                    board[startRow + i][startCol] == board[startRow + i][startCol + 1] &&
                    board[startRow + i][startCol] == board[startRow + i][startCol + 2]) {
                return board[startRow + i][startCol];
            }
        }

        // Check columns
        for (int i = 0; i < 3; i++) {
            if (board[startRow][startCol + i] != 0 &&
                    board[startRow][startCol + i] == board[startRow + 1][startCol + i] &&
                    board[startRow][startCol + i] == board[startRow + 2][startCol + i]) {
                return board[startRow][startCol + i];
            }
        }

        // Check diagonals
        if (board[startRow][startCol] != 0 &&
                board[startRow][startCol] == board[startRow + 1][startCol + 1] &&
                board[startRow][startCol] == board[startRow + 2][startCol + 2]) {
            return board[startRow][startCol];
        }

        if (board[startRow][startCol + 2] != 0 &&
                board[startRow][startCol + 2] == board[startRow + 1][startCol + 1] &&
                board[startRow][startCol + 2] == board[startRow + 2][startCol]) {
            return board[startRow][startCol + 2];
        }

        // Check if the board is full (drawn)
        boolean isFull = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[startRow + i][startCol + j] == 0) {
                    isFull = false;
                    break;
                }
            }
            if (!isFull) {
                break;
            }
        }

        return isFull ? 1 : 0;
    }

    // Check if the game is over and who won (0: not over, 4: X won, 2: O won, 1: drawn)
    public int checkGameStatus() {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (localBoardStatus[i * 3] != 0 &&
                    localBoardStatus[i * 3] != 1 &&
                    localBoardStatus[i * 3] == localBoardStatus[i * 3 + 1] &&
                    localBoardStatus[i * 3] == localBoardStatus[i * 3 + 2]) {
                return localBoardStatus[i * 3];
            }
        }

        // Check columns
        for (int i = 0; i < 3; i++) {
            if (localBoardStatus[i] != 0 &&
                    localBoardStatus[i] != 1 &&
                    localBoardStatus[i] == localBoardStatus[i + 3] &&
                    localBoardStatus[i] == localBoardStatus[i + 6]) {
                return localBoardStatus[i];
            }
        }

        // Check diagonals
        if (localBoardStatus[0] != 0 &&
                localBoardStatus[0] != 1 &&
                localBoardStatus[0] == localBoardStatus[4] &&
                localBoardStatus[0] == localBoardStatus[8]) {
            return localBoardStatus[0];
        }

        if (localBoardStatus[2] != 0 &&
                localBoardStatus[2] != 1 &&
                localBoardStatus[2] == localBoardStatus[4] &&
                localBoardStatus[2] == localBoardStatus[6]) {
            return localBoardStatus[2];
        }

        // Check if the game is drawn (all local boards are won or drawn)
        boolean allBoardsClosed = true;
        for (int i = 0; i < 9; i++) {
            if (localBoardStatus[i] == 0) {
                allBoardsClosed = false;
                break;
            }
        }

        return allBoardsClosed ? 1 : 0;
    }

    // Getters
    public int[][] getBoard() {
        return board;
    }

    public int getNextLocalBoard() {
        return nextLocalBoard;
    }

    public int[] getLocalBoardStatus() {
        return localBoardStatus;
    }

    // Debug method to print the board
    public void printBoard() {
        for (int i = 0; i < 9; i++) {
            if (i % 3 == 0 && i > 0) {
                System.out.println("------+-------+------");
            }

            for (int j = 0; j < 9; j++) {
                if (j % 3 == 0 && j > 0) {
                    System.out.print("| ");
                }

                char symbol = ' ';
                if (board[i][j] == 4) {
                    symbol = 'X';
                } else if (board[i][j] == 2) {
                    symbol = 'O';
                }

                System.out.print(symbol + " ");
            }

            System.out.println();
        }
    }
}