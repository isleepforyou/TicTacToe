import java.util.ArrayList;

class Board {
    // Global board state (which player has won each local board)
    private Mark[] globalBoard;

    // Local boards (9 boards, each with 9 cells)
    private Mark[][] localBoards;

    // Track which local board must be played next (-1 means any board)
    private int nextLocalBoardIndex;

    public Board() {
        // Initialize global board
        globalBoard = new Mark[9];
        for (int i = 0; i < 9; i++) {
            globalBoard[i] = Mark.EMPTY;
        }

        // Initialize local boards
        localBoards = new Mark[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                localBoards[i][j] = Mark.EMPTY;
            }
        }

        // Start with any board being playable
        nextLocalBoardIndex = -1;
    }

    public boolean play(Move move, Mark player) {
        int globalIdx = move.getGlobalIndex();
        int localIdx = move.getLocalIndex();

        // Check if this is a valid board to play on
        if (!isValidBoard(globalIdx)) {
            return false;
        }

        // Check if the cell is empty
        if (localBoards[globalIdx][localIdx] != Mark.EMPTY) {
            return false;
        }

        // Make the move
        localBoards[globalIdx][localIdx] = player;

        // Update local board status if it's won or drawn
        updateLocalBoardStatus(globalIdx);

        // Set next board to play
        nextLocalBoardIndex = localIdx;

        // If the next board is already won or full, allow play on any open board
        if (globalBoard[nextLocalBoardIndex] != Mark.EMPTY) {
            nextLocalBoardIndex = -1;
        }

        return true;
    }

    private boolean isValidBoard(int boardIndex) {
        // If a specific board is required
        if (nextLocalBoardIndex != -1) {
            return boardIndex == nextLocalBoardIndex;
        }

        // If any board is allowed, it must not be already won/drawn
        return globalBoard[boardIndex] == Mark.EMPTY;
    }

    private void updateLocalBoardStatus(int boardIndex) {
        // Check if the local board is won
        Mark winner = checkLocalBoardWinner(boardIndex);
        if (winner != Mark.EMPTY) {
            globalBoard[boardIndex] = winner;
            return;
        }

        // Check if the local board is full (drawn)
        if (isLocalBoardFull(boardIndex)) {
            globalBoard[boardIndex] = Mark.DRAW;
        }
    }

    public boolean isLocalBoardFull(int boardIndex) {
        for (int i = 0; i < 9; i++) {
            if (localBoards[boardIndex][i] == Mark.EMPTY) {
                return false;
            }
        }
        return true;
    }

    public Mark checkLocalBoardWinner(int boardIndex) {
        // Check rows
        for (int row = 0; row < 3; row++) {
            if (localBoards[boardIndex][row*3] != Mark.EMPTY &&
                    localBoards[boardIndex][row*3] == localBoards[boardIndex][row*3 + 1] &&
                    localBoards[boardIndex][row*3 + 1] == localBoards[boardIndex][row*3 + 2]) {
                return localBoards[boardIndex][row*3];
            }
        }

        // Check columns
        for (int col = 0; col < 3; col++) {
            if (localBoards[boardIndex][col] != Mark.EMPTY &&
                    localBoards[boardIndex][col] == localBoards[boardIndex][col + 3] &&
                    localBoards[boardIndex][col + 3] == localBoards[boardIndex][col + 6]) {
                return localBoards[boardIndex][col];
            }
        }

        // Check diagonals
        if (localBoards[boardIndex][0] != Mark.EMPTY &&
                localBoards[boardIndex][0] == localBoards[boardIndex][4] &&
                localBoards[boardIndex][4] == localBoards[boardIndex][8]) {
            return localBoards[boardIndex][0];
        }

        if (localBoards[boardIndex][2] != Mark.EMPTY &&
                localBoards[boardIndex][2] == localBoards[boardIndex][4] &&
                localBoards[boardIndex][4] == localBoards[boardIndex][6]) {
            return localBoards[boardIndex][2];
        }

        return Mark.EMPTY;
    }

    public Mark checkGlobalWinner() {
        // Check rows
        for (int row = 0; row < 3; row++) {
            if (globalBoard[row*3] != Mark.EMPTY && globalBoard[row*3] != Mark.DRAW &&
                    globalBoard[row*3] == globalBoard[row*3 + 1] &&
                    globalBoard[row*3 + 1] == globalBoard[row*3 + 2]) {
                return globalBoard[row*3];
            }
        }

        // Check columns
        for (int col = 0; col < 3; col++) {
            if (globalBoard[col] != Mark.EMPTY && globalBoard[col] != Mark.DRAW &&
                    globalBoard[col] == globalBoard[col + 3] &&
                    globalBoard[col + 3] == globalBoard[col + 6]) {
                return globalBoard[col];
            }
        }

        // Check diagonals
        if (globalBoard[0] != Mark.EMPTY && globalBoard[0] != Mark.DRAW &&
                globalBoard[0] == globalBoard[4] &&
                globalBoard[4] == globalBoard[8]) {
            return globalBoard[0];
        }

        if (globalBoard[2] != Mark.EMPTY && globalBoard[2] != Mark.DRAW &&
                globalBoard[2] == globalBoard[4] &&
                globalBoard[4] == globalBoard[6]) {
            return globalBoard[2];
        }

        return Mark.EMPTY;
    }

    public boolean isGameOver() {
        // Game is over if there's a winner
        if (checkGlobalWinner() != Mark.EMPTY) {
            return true;
        }

        // Game is over if all local boards are won or drawn
        for (int i = 0; i < 9; i++) {
            if (globalBoard[i] == Mark.EMPTY) {
                return false;
            }
        }

        return true;
    }

    public ArrayList<Move> getAvailableMoves() {
        ArrayList<Move> moves = new ArrayList<>();

        // If playing on a specific board is required
        if (nextLocalBoardIndex != -1 && globalBoard[nextLocalBoardIndex] == Mark.EMPTY) {
            for (int i = 0; i < 9; i++) {
                if (localBoards[nextLocalBoardIndex][i] == Mark.EMPTY) {
                    moves.add(new Move(nextLocalBoardIndex, i));
                }
            }
            return moves;
        }

        // If can play on any open board
        for (int board = 0; board < 9; board++) {
            if (globalBoard[board] == Mark.EMPTY) {
                for (int cell = 0; cell < 9; cell++) {
                    if (localBoards[board][cell] == Mark.EMPTY) {
                        moves.add(new Move(board, cell));
                    }
                }
            }
        }

        return moves;
    }

    public Board copy() {
        Board newBoard = new Board();

        // Copy global board state
        for (int i = 0; i < 9; i++) {
            newBoard.globalBoard[i] = this.globalBoard[i];
        }

        // Copy local boards
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                newBoard.localBoards[i][j] = this.localBoards[i][j];
            }
        }

        // Copy next board index
        newBoard.nextLocalBoardIndex = this.nextLocalBoardIndex;

        return newBoard;
    }

    // Getters
    public Mark[] getGlobalBoard() {
        return globalBoard;
    }

    public Mark[][] getLocalBoards() {
        return localBoards;
    }

    public int getNextLocalBoardIndex() {
        return nextLocalBoardIndex;
    }
}