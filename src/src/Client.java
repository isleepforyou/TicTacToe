import java.io.*;
import java.net.*;

/**
 * Client pour jeu Ultimate Tic-Tac-Toe
 */
public class Client {
    private static final int PLAYER_X = 4;
    private static final int PLAYER_O = 2;
    private static final long TIME_LIMIT_MILLIS = 2800; // 2.8 secondes

    private Socket socket;
    private BufferedInputStream input;
    private BufferedOutputStream output;
    private BufferedReader console;
    private Board board;
    private int player; // 4 pour X, 2 pour O

    public Client(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        input = new BufferedInputStream(socket.getInputStream());
        output = new BufferedOutputStream(socket.getOutputStream());
        console = new BufferedReader(new InputStreamReader(System.in));
        board = new Board();
    }

    public void play() {
        try {
            while (true) {
                char cmd = (char) input.read();
                System.out.println("Received command: " + cmd);

                if (cmd == '1') {
                    // Joue en tant que X
                    player = PLAYER_X;
                    handleStartGame();

                    // X joue en premier
                    makeAIMove();
                } else if (cmd == '2') {
                    // Joue en tant que O
                    player = PLAYER_O;
                    handleStartGame();
                    System.out.println("Waiting for X's move...");
                } else if (cmd == '3') {
                    // Serveur demande le prochain coup
                    byte[] aBuffer = new byte[16];
                    int size = input.available();
                    System.out.println("Size: " + size);
                    input.read(aBuffer, 0, size);

                    String lastMoveStr = new String(aBuffer).trim();
                    System.out.println("Last move: " + lastMoveStr);

                    // Met à jour le plateau avec le coup adverse
                    Move lastMove = MoveGenerator.parseMove(lastMoveStr);
                    if (lastMove != null && !lastMoveStr.equals("A0")) {
                        int opponent = (player == PLAYER_X) ? PLAYER_O : PLAYER_X;
                        board.makeMove(lastMove.getRow(), lastMove.getCol(), opponent);
                        System.out.println("Updated board with opponent's move: " + lastMoveStr);
                        board.printBoard();
                    }

                    // Joue notre coup
                    makeAIMove();
                } else if (cmd == '4') {
                    // Coup invalide
                    System.out.println("Invalid move! Trying again...");
                    makeAIMove();
                } else if (cmd == '5') {
                    // Fin de partie
                    byte[] aBuffer = new byte[16];
                    int size = input.available();
                    input.read(aBuffer, 0, size);
                    String lastMoveStr = new String(aBuffer).trim();
                    System.out.println("Game over. Last move: " + lastMoveStr);

                    // Envoi d'un retour à la ligne pour accuser réception
                    output.write("\n".getBytes(), 0, 1);
                    output.flush();
                    break;
                } else {
                    System.out.println("Unknown command: " + cmd);
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleStartGame() throws IOException {
        byte[] aBuffer = new byte[1024];
        int size = input.available();
        System.out.println("Size: " + size);
        input.read(aBuffer, 0, size);

        String boardString = new String(aBuffer).trim();
        System.out.println("Board: " + boardString);

        // Initialise le plateau
        String[] boardValues = boardString.split(" ");
        int[] boardInts = new int[boardValues.length];
        for (int i = 0; i < boardValues.length; i++) {
            boardInts[i] = Integer.parseInt(boardValues[i]);
        }

        board.initializeBoard(boardInts);

        System.out.println("New game started! You are playing " + (player == PLAYER_X ? "X" : "O"));
        board.printBoard();
    }

    private void makeAIMove() throws IOException {
        System.out.println("AI thinking...");
        long startTime = System.currentTimeMillis();

        // Trouve le meilleur coup
        Move bestMove = MinimaxAlphaBeta.findBestMove(board, player, TIME_LIMIT_MILLIS);

        if (bestMove != null) {
            // Convertit le coup en chaîne
            String moveStr = MoveGenerator.formatMove(bestMove);
            System.out.println("AI's move: " + moveStr);

            // Joue le coup sur notre plateau
            board.makeMove(bestMove.getRow(), bestMove.getCol(), player);
            board.printBoard();

            // Envoie le coup au serveur
            output.write(moveStr.getBytes(), 0, moveStr.length());
            output.flush();

            long endTime = System.currentTimeMillis();
            System.out.println("Time taken: " + (endTime - startTime) + " ms");
        } else {
            System.out.println("No valid moves found!");
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 8888;

        // Analyse des arguments
        if (args.length > 0) {
            serverAddress = args[0];
        }

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Using default: 8888");
            }
        }

        try {
            Client client = new Client(serverAddress, port);
            System.out.println("Starting game with AI player");
            client.play();
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}