import java.io.*;
import java.net.*;

class Client {
    private static Board board;
    private static MiniMax miniMax;
    private static Mark cpuMark;

    public static void main(String[] args) {
        // Determine player mark (X or O)
        if (args.length > 0 && args[0].equalsIgnoreCase("O")) {
            cpuMark = Mark.O;
        } else {
            cpuMark = Mark.X;
        }

        // Initialize board and AI
        board = new Board();

        // Set max depth based on system capabilities
        int maxDepth = Runtime.getRuntime().availableProcessors() > 1 ? 8 : 6;
        miniMax = new MiniMax(cpuMark, maxDepth);

        // Server connection details
        String serverAddress = "localhost";
        if (args.length > 1) {
            serverAddress = args[1];
        }

        try {
            Socket socket = new Socket(serverAddress, 8888);
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());

            while (true) {
                char cmd = (char)input.read();

                // Start game as player X (first)
                if (cmd == '1') {
                    byte[] buffer = new byte[1024];
                    int size = input.available();
                    input.read(buffer, 0, size);
                    String boardState = new String(buffer).trim();

                    // Update board from server state
                    updateBoardFromString(boardState);

                    // Get AI move
                    Move move = miniMax.findBestMove(board);
                    String moveStr = moveToString(move);

                    // Send move to server
                    output.write(moveStr.getBytes(), 0, moveStr.length());
                    output.flush();
                }

                // Start game as player O (second)
                else if (cmd == '2') {
                    byte[] buffer = new byte[1024];
                    int size = input.available();
                    input.read(buffer, 0, size);
                    String boardState = new String(buffer).trim();

                    // Update board
                    updateBoardFromString(boardState);
                }

                // Server requests next move
                else if (cmd == '3') {
                    byte[] buffer = new byte[16];
                    int size = input.available();
                    input.read(buffer, 0, size);
                    String opponentMove = new String(buffer).trim();

                    // Apply opponent's move
                    if (!opponentMove.equals("A0")) {
                        applyOpponentMove(opponentMove);
                    }

                    // Calculate our move
                    Move move = miniMax.findBestMove(board);
                    String moveStr = moveToString(move);

                    // Apply our move to our board
                    board.play(move, cpuMark);

                    // Send move to server
                    output.write(moveStr.getBytes(), 0, moveStr.length());
                    output.flush();
                }

                // Invalid move
                else if (cmd == '4') {
                    // Get a new move
                    Move move = miniMax.findBestMove(board);
                    String moveStr = moveToString(move);

                    // Apply new move to our board
                    board.play(move, cpuMark);

                    // Send move to server
                    output.write(moveStr.getBytes(), 0, moveStr.length());
                    output.flush();
                }

                // Game over
                else if (cmd == '5') {
                    byte[] buffer = new byte[16];
                    int size = input.available();
                    input.read(buffer, 0, size);

                    // Reset for next game
                    board = new Board();

                    // Send ready signal
                    output.write("\n".getBytes(), 0, 1);
                    output.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Convert server notation (e.g., "A1") to a Move object
    private static Move stringToMove(String moveStr) {
        if (moveStr.length() >= 2) {
            char colChar = moveStr.charAt(0);
            char rowChar = moveStr.charAt(1);

            // Determine the global and local indices
            // This would need to be modified based on your exact server protocol
            int globalRow = (rowChar - '1') / 3;
            int globalCol = (colChar - 'A') / 3;
            int globalIndex = globalRow * 3 + globalCol;

            int localRow = (rowChar - '1') % 3;
            int localCol = (colChar - 'A') % 3;
            int localIndex = localRow * 3 + localCol;

            return new Move(globalIndex, localIndex);
        }
        return null;
    }

    // Convert a Move object to server notation (e.g., "A1")
    private static String moveToString(Move move) {
        if (move == null) {
            return "A1"; // Default move
        }

        int globalIndex = move.getGlobalIndex();
        int localIndex = move.getLocalIndex();

        // Determine the global position
        int globalRow = globalIndex / 3;
        int globalCol = globalIndex % 3;

        // Determine the local position
        int localRow = localIndex / 3;
        int localCol = localIndex % 3;

        // Calculate the absolute row and column
        int row = globalRow * 3 + localRow;
        int col = globalCol * 3 + localCol;

        // Convert to server notation
        char colChar = (char)('A' + col);
        char rowChar = (char)('1' + row);

        return String.valueOf(colChar) + rowChar;
    }

    // Apply opponent's move to our board
    private static void applyOpponentMove(String moveStr) {
        Move move = stringToMove(moveStr);
        Mark opponentMark = (cpuMark == Mark.X) ? Mark.O : Mark.X;
        board.play(move, opponentMark);
    }

    // Update board state from server string
    private static void updateBoardFromString(String boardStr) {
        // This would need to be implemented based on your server protocol
        // For example, parsing a string representation of the board state

        // Reset the board
        board = new Board();

        // Example implementation (modify based on actual protocol):
        String[] values = boardStr.split(" ");
        if (values.length < 81) return; // Not enough values

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                int index = i * 9 + j;
                int globalIndex = (i / 3) * 3 + (j / 3);
                int localIndex = (i % 3) * 3 + (j % 3);

                int value = Integer.parseInt(values[index]);
                if (value == 4) { // X
                    board.getLocalBoards()[globalIndex][localIndex] = Mark.X;
                } else if (value == 2) { // O
                    board.getLocalBoards()[globalIndex][localIndex] = Mark.O;
                }
            }
        }

        // Update global board status for all local boards
        for (int i = 0; i < 9; i++) {
            Mark localWinner = board.checkLocalBoardWinner(i);
            if (localWinner != Mark.EMPTY) {
                board.getGlobalBoard()[i] = localWinner;
            } else if (board.isLocalBoardFull(i)) {
                board.getGlobalBoard()[i] = Mark.DRAW;
            }
        }
    }
}