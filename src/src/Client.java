import java.io.*;
import java.net.*;
import java.util.ArrayList;

class Client {
    private static Board board;
    private static CPUPlayer cpu;
    private static Mark cpuMark;
    private static Mark opponentMark;

    public static void main(String[] args) {
        Socket MyClient;
        BufferedInputStream input;
        BufferedOutputStream output;

        // Initialiser le plateau
        board = new Board();

        // Déterminer quel joueur nous sommes (X ou O) en fonction des arguments
        if (args.length > 0 && args[0].equalsIgnoreCase("O")) {
            cpuMark = Mark.O;
            opponentMark = Mark.X;
        } else {
            cpuMark = Mark.X;
            opponentMark = Mark.O;
        }

        // Créer l'IA
        cpu = new CPUPlayer(cpuMark);

        // Adresse du serveur
        String serverAddress = "localhost";
        if (args.length > 1) {
            serverAddress = args[1];
        }

        try {
            MyClient = new Socket(serverAddress, 8888);

            input = new BufferedInputStream(MyClient.getInputStream());
            output = new BufferedOutputStream(MyClient.getOutputStream());

            while(true) {
                char cmd = (char)input.read();

                // Début de la partie en joueur X (premier)
                if(cmd == '1') {
                    byte[] aBuffer = new byte[1024];
                    int size = input.available();
                    input.read(aBuffer, 0, size);
                    String s = new String(aBuffer).trim();

                    // Mise à jour du plateau
                    updateBoardFromString(s);

                    // Obtenir le coup de l'IA
                    String move = getNextCPUMove();

                    output.write(move.getBytes(), 0, move.length());
                    output.flush();
                }

                // Début de la partie en joueur O (second)
                if(cmd == '2') {
                    byte[] aBuffer = new byte[1024];
                    int size = input.available();
                    input.read(aBuffer, 0, size);
                    String s = new String(aBuffer).trim();

                    // Mise à jour du plateau
                    updateBoardFromString(s);
                }

                // Le serveur demande le prochain coup
                if(cmd == '3') {
                    byte[] aBuffer = new byte[16];
                    int size = input.available();
                    input.read(aBuffer, 0, size);
                    String s = new String(aBuffer).trim();

                    // Mise à jour du plateau avec le coup de l'adversaire
                    if (!s.equals("A0")) { // A0 est un coup invalide pour l'initialisation
                        updateBoardWithOpponentMove(s);
                    }

                    // Obtenir le coup de l'IA
                    String move = getNextCPUMove();

                    output.write(move.getBytes(), 0, move.length());
                    output.flush();
                }

                // Le dernier coup est invalide
                if(cmd == '4') {
                    // Obtenir un nouveau coup de l'IA
                    String move = getNextCPUMove();

                    output.write(move.getBytes(), 0, move.length());
                    output.flush();
                }

                // La partie est terminée
                if(cmd == '5') {
                    byte[] aBuffer = new byte[16];
                    int size = input.available();
                    input.read(aBuffer, 0, size);

                    // Libérer les ressources du pool de threads
                    cpu.shutdown();

                    // Réinitialiser le plateau pour une nouvelle partie
                    board = new Board();
                    cpu = new CPUPlayer(cpuMark);

                    // Continuer automatiquement
                    output.write("\n".getBytes(), 0, 1);
                    output.flush();
                }
            }
        }
        catch (IOException e) {
            System.out.println("Erreur: " + e);
            if (cpu != null) {
                cpu.shutdown();
            }
        }
    }

    // Convertit la notation serveur (ex: "A1") en objet Move
    private static Move stringToMove(String moveStr) {
        if (moveStr.length() >= 2) {
            char colChar = moveStr.charAt(0);
            char rowChar = moveStr.charAt(1);

            int col = colChar - 'A';
            int row = rowChar - '1';

            return new Move(row, col);
        }
        return new Move();
    }

    // Convertit un objet Move en notation serveur (ex: "A1")
    private static String moveToString(Move move) {
        char colChar = (char) ('A' + move.getCol());
        char rowChar = (char) ('1' + move.getRow());
        return String.valueOf(colChar) + rowChar;
    }

    // Met à jour le plateau avec le coup de l'adversaire
    private static void updateBoardWithOpponentMove(String moveStr) {
        Move move = stringToMove(moveStr);
        board.play(move, opponentMark);
    }

    // Obtient le prochain coup de l'IA
    private static String getNextCPUMove() {
        ArrayList<Move> bestMoves = cpu.getNextMoveAB(board);

        if (bestMoves.isEmpty()) {
            // Chercher un coup valide par défaut
            ArrayList<Move> availableMoves = board.getAvailableMoves();
            if (!availableMoves.isEmpty()) {
                Move defaultMove = availableMoves.get(0);
                String moveStr = moveToString(defaultMove);
                board.play(defaultMove, cpuMark);
                return moveStr;
            }
            return "A1"; // Coup par défaut en cas de problème
        }

        // Choisir un coup aléatoire parmi les meilleurs
        int index = (int) (Math.random() * bestMoves.size());
        Move selectedMove = bestMoves.get(index);

        // Mettre à jour notre représentation du plateau
        board.play(selectedMove, cpuMark);

        return moveToString(selectedMove);
    }

    // Met à jour le plateau à partir de la chaîne reçue du serveur
    private static void updateBoardFromString(String boardStr) {
        String[] boardValues = boardStr.split(" ");

        // Réinitialiser le plateau
        board = new Board();

        // Si la chaîne est vide ou ne contient pas assez de valeurs, retourner
        if (boardValues.length < 81) {
            return;
        }

        // Mettre à jour le plateau avec les valeurs reçues
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                int index = y * 9 + x;
                if (index < boardValues.length) {
                    int value = Integer.parseInt(boardValues[index]);
                    if (value == 4) {
                        board.getGrid()[y][x] = Mark.X;
                    } else if (value == 2) {
                        board.getGrid()[y][x] = Mark.O;
                    }
                }
            }
        }
    }
}