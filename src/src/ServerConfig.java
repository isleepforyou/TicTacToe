import javax.swing.JOptionPane;

/**
 * Gère la configuration du serveur pour le jeu Ultimate Tic-Tac-Toe
 * Cette classe permet de spécifier l'adresse IP et le port via:
 * - Arguments en ligne de commande
 * - Fenêtres de dialogue interactives
 */
public class ServerConfig {
    private static final String DEFAULT_ADDRESS = "localhost";
    private static final int DEFAULT_PORT = 8888;

    private String serverAddress;
    private int port;

    /**
     * Initialise la configuration avec les valeurs par défaut
     * ou les arguments fournis. Si aucun argument n'est fourni,
     * des boîtes de dialogue sont affichées pour saisir les valeurs.
     *
     * @param args Arguments de ligne de commande (args[0]=adresse, args[1]=port)
     */
    public ServerConfig(String[] args) {
        serverAddress = DEFAULT_ADDRESS;
        port = DEFAULT_PORT;

        // Si des arguments sont fournis, les utiliser
        if (args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
            serverAddress = args[0];
        } else {
            // Si aucun argument d'adresse fourni, demander via interface graphique
            promptServerAddress();
        }

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Using default: " + DEFAULT_PORT);

                // Si le port est invalide, demander via interface graphique
                promptServerPort();
            }
        } else {
            // Si aucun argument de port fourni, demander via interface graphique
            promptServerPort();
        }

        // Affiche les paramètres de configuration finaux
        System.out.println("Server configuration:");
        System.out.println("Address: " + serverAddress);
        System.out.println("Port: " + port);
    }

    /**
     * Affiche une fenêtre de dialogue pour configurer l'adresse du serveur
     * Utilise localhost par défaut si l'utilisateur ne saisit rien
     */
    private void promptServerAddress() {
        String input = JOptionPane.showInputDialog(
                null,
                "Enter server address (or leave empty for localhost):",
                "Server Configuration",
                JOptionPane.QUESTION_MESSAGE);

        // Si l'utilisateur a entré quelque chose (et n'a pas annulé)
        if (input != null && !input.trim().isEmpty()) {
            serverAddress = input.trim();
        }
    }

    /**
     * Affiche une fenêtre de dialogue pour configurer le port du serveur
     * Utilise le port par défaut (8888) si l'utilisateur ne saisit rien
     * ou si la saisie n'est pas un nombre valide
     */
    private void promptServerPort() {
        String input = JOptionPane.showInputDialog(
                null,
                "Enter server port (or leave empty for default: " + DEFAULT_PORT + "):",
                "Server Configuration",
                JOptionPane.QUESTION_MESSAGE);

        // Si l'utilisateur a entré quelque chose (et n'a pas annulé)
        if (input != null && !input.trim().isEmpty()) {
            try {
                port = Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Invalid port number. Using default: " + DEFAULT_PORT,
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Retourne l'adresse du serveur
     * @return Adresse du serveur
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Retourne le port du serveur
     * @return Port du serveur
     */
    public int getPort() {
        return port;
    }
}