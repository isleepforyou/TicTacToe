import java.io.*;
import java.net.*;
import java.util.List;
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

    // Statistiques de jeu simples
    private int validMovesReceived = 0;
    private int invalidMovesReceived = 0;

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
                    System.out.println("Playing as X (first player)");

                    // X joue en premier
                    makeAIMove();
                } else if (cmd == '2') {
                    // Joue en tant que O
                    player = PLAYER_O;
                    handleStartGame();
                    System.out.println("Playing as O (second player)");
                    System.out.println("Waiting for X's move...");
                } else if (cmd == '3') {
                    // Serveur demande le prochain coup
                    byte[] aBuffer = new byte[16];
                    int size = input.available();
                    System.out.println("Available bytes: " + size);
                    input.read(aBuffer, 0, size);

                    String lastMoveStr = new String(aBuffer).trim();
                    System.out.println("Received opponent move: " + lastMoveStr);

                    // Met à jour le plateau avec le coup adverse
                    processOpponentMove(lastMoveStr);

                    // Joue notre coup
                    makeAIMove();
                } else if (cmd == '4') {
                    // Coup invalide
                    System.out.println("Server rejected our move as invalid! Trying again...");
                    makeAIMove();
                } else if (cmd == '5') {
                    // Fin de partie
                    byte[] aBuffer = new byte[16];
                    int size = input.available();
                    input.read(aBuffer, 0, size);
                    String lastMoveStr = new String(aBuffer).trim();
                    System.out.println("Game over. Last move: " + lastMoveStr);

                    // Affiche les statistiques de validation
                    System.out.println("Game statistics:");
                    System.out.println("- Valid opponent moves: " + validMovesReceived);
                    System.out.println("- Invalid opponent moves: " + invalidMovesReceived);

                    // Envoi d'un retour à la ligne pour accuser réception
                    output.write("\n".getBytes(), 0, 1);
                    output.flush();
                    break;
                } else {
                    System.out.println("Unknown command received: " + cmd);
                }
            }
        } catch (IOException e) {
            System.out.println("Communication error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Traite et valide un coup reçu de l'adversaire
     *
     * @param moveStr La chaîne représentant le coup (ex: "D6")
     * @return true si le coup a été accepté et appliqué, false sinon
     */
    private boolean processOpponentMove(String moveStr) {
        // Cas spécial : "A0" est une notation spéciale (à ignorer)
        if (moveStr.equals("A0")) {
            System.out.println("Special move A0 received (ignored)");
            return true;
        }

        // Valide le format du coup
        if (!isValidMoveFormat(moveStr)) {
            System.out.println("Warning: Received invalid move format: " + moveStr);
            invalidMovesReceived++;
            return false;
        }

        // Parser le coup
        Move move = MoveGenerator.parseMove(moveStr);
        if (move == null) {
            System.out.println("Warning: Failed to parse move: " + moveStr);
            invalidMovesReceived++;
            return false;
        }

        // Vérifie si le coup est valide selon les règles du jeu
        if (!board.isValidMove(move.getRow(), move.getCol())) {
            System.out.println("Warning: Received illegal move: " + moveStr);
            invalidMovesReceived++;
            return false;
        }

        // Applique le coup
        int opponent = (player == PLAYER_X) ? PLAYER_O : PLAYER_X;
        boolean success = board.makeMove(move.getRow(), move.getCol(), opponent);
        if (success) {
            validMovesReceived++;
            System.out.println("Opponent's move accepted: " + moveStr);
            board.printBoard();
            return true;
        } else {
            // Ca ne devrait pas arriver
            System.out.println("Error: Move validation inconsistency for: " + moveStr);
            invalidMovesReceived++;
            return false;
        }
    }

    /**
     * Vérifie le format d'une chaîne représentant un coup
     * @param moveStr Chaîne à valider (ex: "D6")
     * @return true si le format est valide, false sinon
     */
    private boolean isValidMoveFormat(String moveStr) {
        if (moveStr == null || moveStr.isEmpty()) {
            return false;
        }

        // Format valide: une lettre A-I suivie d'un chiffre 1-9
        return moveStr.matches("^[A-Ia-i][1-9]$");
    }

    private void handleStartGame() throws IOException {
        byte[] aBuffer = new byte[1024];
        int size = input.available();
        System.out.println("Size: " + size);
        input.read(aBuffer, 0, size);

        String boardString = new String(aBuffer).trim();
        System.out.println("Board: " + boardString);

        // Initialise le plateau
        try {
            String[] boardValues = boardString.split(" ");
            int[] boardInts = new int[boardValues.length];
            for (int i = 0; i < boardValues.length; i++) {
                boardInts[i] = Integer.parseInt(boardValues[i]);
            }

            board.initializeBoard(boardInts);
            System.out.println("New game started! You are playing " + (player == PLAYER_X ? "X" : "O"));
            board.printBoard();

            // Réinitialise les statistiques
            validMovesReceived = 0;
            invalidMovesReceived = 0;
        } catch (NumberFormatException e) {
            System.out.println("Failed to parse board data: " + e.getMessage());
        }
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

        try {
            // Trouve le meilleur coup
            Move bestMove = MinimaxAlphaBeta.findBestMove(board, player, TIME_LIMIT_MILLIS);

            if (bestMove != null) {
                // Vérifie que le coup est valide
                if (!board.isValidMove(bestMove.getRow(), bestMove.getCol())) {
                    System.out.println("Warning: AI generated an invalid move! Finding alternative...");
                    bestMove = findRandomValidMove();
                }

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
                    handleNoValidMoves();
                }
            } else {
                handleNoValidMoves();
            }
        } catch (Exception e) {
            System.out.println("AI error: " + e.getMessage());
            e.printStackTrace();
            handleNoValidMoves();
        }
    }

    /**
     * Gère le cas où aucun coup valide n'est trouvé
     */
    private void handleNoValidMoves() throws IOException {
        System.out.println("No valid moves found!");

        // Recherche d'un coup de secours
        Move fallbackMove = findRandomValidMove();

        if (fallbackMove != null) {
            String moveStr = MoveGenerator.formatMove(fallbackMove);
            System.out.println("Using fallback move: " + moveStr);

            // Applique et envoie le coup
            board.makeMove(fallbackMove.getRow(), fallbackMove.getCol(), player);
            output.write(moveStr.getBytes(), 0, moveStr.length());
            output.flush();
        } else {
            // Vraiment aucun coup possible, envoie code spécial
            String defaultResponse = "A0";
            System.out.println("No valid moves possible. Sending special code: " + defaultResponse);
            output.write(defaultResponse.getBytes(), 0, defaultResponse.length());
            output.flush();
        }
    }

    /**
     * Trouve un coup valide aléatoire (pour les cas d'urgence)
     */
    private Move findRandomValidMove() {
        List<Move> validMoves = MoveGenerator.generateMoves(board);
        if (validMoves.isEmpty()) {
            return null;
        }

        // Prend simplement le premier coup valide
        return validMoves.get(0);
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