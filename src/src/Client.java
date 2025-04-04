import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;

/**
 * Client pour jeu Ultimate Tic-Tac-Toe
 * Gère la communication avec le serveur et l'exécution de l'IA
 */
public class Client {
    private static final int PLAYER_X = 4;
    private static final int PLAYER_O = 2;
    private static final long TIME_LIMIT_MILLIS = 2900; // 2.9 secondes

    private Socket socket;
    private BufferedInputStream input;
    private BufferedOutputStream output;
    private BufferedReader console;
    private Board board;
    private int player; // 4 pour X, 2 pour O

    /**
     * Établit une connexion avec le serveur de jeu
     *
     * @param serverAddress Adresse IP ou nom d'hôte du serveur
     * @param port Port du serveur
     * @throws IOException En cas d'erreur de connexion
     */
    public Client(String serverAddress, int port) throws IOException {
        // Initialise la connexion réseau
        socket = new Socket(serverAddress, port);
        input = new BufferedInputStream(socket.getInputStream());
        output = new BufferedOutputStream(socket.getOutputStream());
        console = new BufferedReader(new InputStreamReader(System.in));

        // Initialise le plateau de jeu
        board = new Board();
    }

    /**
     * Boucle principale de jeu.
     * Traite les commandes reçues du serveur et y répond.
     */
    public void play() {
        try {
            while (true) {
                // Lit la commande envoyée par le serveur
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

    /**
     * Calcule et envoie le prochain coup de l'IA au serveur
     * Utilise l'algorithme Minimax avec élagage alpha-beta
     *
     * @throws IOException En cas d'erreur de communication avec le serveur
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

    public static void main(String[] args) {
        try {
            // Obtient la configuration du serveur (via arguments ou boîtes de dialogue)
            ServerConfig config = new ServerConfig(args);
            String serverAddress = config.getServerAddress();
            int port = config.getPort();

            // Tente de se connecter au serveur avec les paramètres spécifiés
            System.out.println("Connecting to server at " + serverAddress + ":" + port);
            Client client = new Client(serverAddress, port);

            // Démarre la partie si la connexion est établie
            System.out.println("Starting game with AI player");
            client.play();
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();

            // Affiche une boîte de dialogue en cas d'erreur de connexion
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to connect to server: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}