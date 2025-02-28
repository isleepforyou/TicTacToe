import java.io.*;
import java.net.*;

/**
 * Client pour jeu Ultimate Tic-Tac-Toe
 */
public class Client {
    // Constantes du jeu
    private static final int PLAYER_X = 4;
    private static final int PLAYER_O = 2;
    private static final long TIME_LIMIT_MILLIS = 2900; // 2.9 secondes

    // Constantes pour les commandes du serveur
    private static final char CMD_PLAY_AS_X = '1';
    private static final char CMD_PLAY_AS_O = '2';
    private static final char CMD_REQUEST_MOVE = '3';
    private static final char CMD_INVALID_MOVE = '4';
    private static final char CMD_GAME_OVER = '5';

    // Variables de connexion
    private Socket socket;
    private BufferedInputStream input;
    private BufferedOutputStream output;
    private BufferedReader console;

    // État du jeu
    private Board board;
    private int player; // 4 pour X, 2 pour O

    /**
     * Initialise le client et établit la connexion au serveur
     */
    public Client(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        input = new BufferedInputStream(socket.getInputStream());
        output = new BufferedOutputStream(socket.getOutputStream());
        console = new BufferedReader(new InputStreamReader(System.in));
        board = new Board();
    }

    /**
     * Boucle principale du jeu
     */
    public void play() {
        try {
            while (true) {
                char cmd = (char) input.read();
                System.out.println("Received command: " + cmd);

                switch (cmd) {
                    case CMD_PLAY_AS_X:
                        // Joue en tant que X (premier joueur)
                        player = PLAYER_X;
                        handleStartGame();
                        makeAIMove(); // X joue en premier
                        break;

                    case CMD_PLAY_AS_O:
                        // Joue en tant que O (second joueur)
                        player = PLAYER_O;
                        handleStartGame();
                        System.out.println("Waiting for X's move...");
                        break;

                    case CMD_REQUEST_MOVE:
                        // Serveur demande le prochain coup
                        handleOpponentMove();
                        makeAIMove();
                        break;

                    case CMD_INVALID_MOVE:
                        // Coup invalide
                        System.out.println("Invalid move! Trying again...");
                        makeAIMove();
                        break;

                    case CMD_GAME_OVER:
                        // Fin de partie
                        handleGameOver();
                        return; // Quitte la boucle et termine le programme

                    default:
                        System.out.println("Unknown command: " + cmd);
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }

    /**
     * Gère le début d'une partie
     */
    private void handleStartGame() throws IOException {
        // Lecture de l'état initial du plateau
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

    /**
     * Gère la réception du coup de l'adversaire
     */
    private void handleOpponentMove() throws IOException {
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
    }

    /**
     * Gère la fin de partie
     */
    private void handleGameOver() throws IOException {
        byte[] aBuffer = new byte[16];
        int size = input.available();
        input.read(aBuffer, 0, size);
        String lastMoveStr = new String(aBuffer).trim();
        System.out.println("Game over. Last move: " + lastMoveStr);

        // Envoi d'un retour à la ligne pour accuser réception
        output.write("\n".getBytes(), 0, 1);
        output.flush();
    }

    /**
     * Calcule et effectue le coup de l'IA
     */
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

    /**
     * Ferme proprement les ressources
     */
    private void closeResources() {
        try {
            if (console != null) console.close();
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Error closing resources: " + e.getMessage());
        }
    }

    /**
     * Point d'entrée principal
     */
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