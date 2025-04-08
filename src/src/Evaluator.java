/**
 * Évaluateur amélioré pour jeu Ultimate Tic-Tac-Toe avec Minimax
 */
public class Evaluator {
    // Constantes pour l'évaluation
    private static final int WIN_SCORE = 100000;
    private static final int POTENTIAL_WIN_SCORE = 1000;
    private static final int TWO_IN_A_ROW_SCORE = 100;
    private static final int STRATEGIC_LOCAL_BOARD_SCORE = 500;

    // Valeurs des positions dans plateau local (optimisées)
    private static final int CENTER_SCORE = 6;
    private static final int CORNER_SCORE = 4;
    private static final int EDGE_SCORE = 2;

    // Valeurs des plateaux locaux
    private static final int CENTER_BOARD_BONUS = 4;
    private static final int CORNER_BOARD_BONUS = 3;
    private static final int EDGE_BOARD_BONUS = 2;

    // Poids des positions pour plateaux locaux (optimisés)
    private static final int[][] POSITION_WEIGHTS = {
            {4, 2, 4},
            {2, 6, 2},
            {4, 2, 4}
    };

    // Poids des plateaux locaux (optimisés)
    private static final int[][] BOARD_WEIGHTS = {
            {3, 2, 3},
            {2, 4, 2},
            {3, 2, 3}
    };

    // Table de transposition (pour mémoriser les évaluations précédentes)
    private static final java.util.Map<String, Integer> transpositionTable = new java.util.HashMap<>();

    /**
     * Génère une clé pour la table de transposition
     */
    public static String generateBoardKey(Board board) {
        StringBuilder key = new StringBuilder();
        int[][] boardState = board.getBoard();

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                key.append(boardState[i][j]);
            }
        }

        key.append("|").append(board.getNextLocalBoard());
        return key.toString();
    }

    /**
     * Vérifie si une évaluation existe déjà dans la table
     */
    public static Integer getCachedEvaluation(Board board) {
        return transpositionTable.get(generateBoardKey(board));
    }

    /**
     * Stocke une évaluation dans la table
     */
    public static void cacheEvaluation(Board board, int score) {
        // Limiter la taille de la table pour éviter la surcharge mémoire
        if (transpositionTable.size() > 10000) {
            transpositionTable.clear();
        }
        transpositionTable.put(generateBoardKey(board), score);
    }

    // Évalue la position pour le joueur (4 pour X, 2 pour O)
    public static int evaluate(Board board, int player) {
        // Vérification de la table de transposition
        Integer cachedScore = getCachedEvaluation(board);
        if (cachedScore != null) {
            return cachedScore;
        }

        int opponent = (player == 4) ? 2 : 4;

        // Optimisation: Vérifie d'abord si la partie est terminée
        int gameStatus = board.checkGameStatus();
        if (gameStatus == player) {
            return WIN_SCORE;
        } else if (gameStatus == opponent) {
            return -WIN_SCORE;
        } else if (gameStatus == 1) {
            return 0; // Match nul
        }

        int score = 0;
        int[][] boardState = board.getBoard();
        int[] localBoardStatus = board.getLocalBoardStatus();

        // Menaces et victoires potentielles
        int playerLocalWins = 0;
        int opponentLocalWins = 0;

        // Facteurs d'urgence - priorité plus élevée aux situations critiques
        int urgentDefenseNeeded = 0;
        int urgentAttackPossible = 0;

        // Évalue chaque plateau local
        for (int boardRow = 0; boardRow < 3; boardRow++) {
            for (int boardCol = 0; boardCol < 3; boardCol++) {
                int localBoard = boardRow * 3 + boardCol;
                int boardWeight = BOARD_WEIGHTS[boardRow][boardCol];

                // Position de départ de ce plateau local
                int startRow = boardRow * 3;
                int startCol = boardCol * 3;

                // Si le plateau local est gagné
                if (localBoardStatus[localBoard] == player) {
                    score += STRATEGIC_LOCAL_BOARD_SCORE * boardWeight;
                    playerLocalWins++;
                } else if (localBoardStatus[localBoard] == opponent) {
                    score -= STRATEGIC_LOCAL_BOARD_SCORE * boardWeight;
                    opponentLocalWins++;
                } else if (localBoardStatus[localBoard] == 0) {
                    // Évalue un plateau local ouvert
                    int localScore = evaluateLocalBoard(boardState, startRow, startCol, player, opponent, boardWeight);
                    score += localScore;

                    // Détection des situations urgentes
                    if (localScore > POTENTIAL_WIN_SCORE / 2) {
                        urgentAttackPossible++;
                    } else if (localScore < -POTENTIAL_WIN_SCORE / 2) {
                        urgentDefenseNeeded++;
                    }
                }
            }
        }

        // Évalue les motifs globaux avec plus de poids pour les motifs critiques
        score += evaluateGlobalPatterns(localBoardStatus, player, opponent);

        // Évalue les coups forcés et la sélection stratégique du plateau
        int nextLocalBoard = board.getNextLocalBoard();
        if (nextLocalBoard != -1) {
            score += evaluateForcedMove(localBoardStatus, nextLocalBoard, playerLocalWins, opponentLocalWins);
        }

        // Évalue les situations d'urgence - critique en minimax
        if (urgentDefenseNeeded > 0) {
            score -= 500 * urgentDefenseNeeded; // Priorité à la défense
        }

        if (urgentAttackPossible > 0) {
            score += 400 * urgentAttackPossible;
        }

        // Évaluation des menaces de victoire globales - ajusté
        if (playerLocalWins >= 2) {
            score += playerLocalWins * 150;

            // Bonus pour configuration en diagonale ou ligne
            score += evaluateWinningConfiguration(localBoardStatus, player) * 50;
        }

        if (opponentLocalWins >= 2) {
            score -= opponentLocalWins * 200; // Priorité plus forte à la défense

            // Malus accru pour configuration adverse dangereuse
            score -= evaluateWinningConfiguration(localBoardStatus, opponent) * 80;
        }

        // Cache le résultat avant de retourner
        cacheEvaluation(board, score);
        return score;
    }

    /**
     * Évalue la configuration des plateaux gagnés (alignement)
     */
    private static int evaluateWinningConfiguration(int[] localBoardStatus, int player) {
        int score = 0;

        // Vérifie les lignes
        for (int i = 0; i < 3; i++) {
            int count = 0;
            for (int j = 0; j < 3; j++) {
                if (localBoardStatus[i*3 + j] == player) count++;
            }
            if (count == 2) score += 2;
        }

        // Vérifie les colonnes
        for (int j = 0; j < 3; j++) {
            int count = 0;
            for (int i = 0; i < 3; i++) {
                if (localBoardStatus[i*3 + j] == player) count++;
            }
            if (count == 2) score += 2;
        }

        // Vérifie les diagonales
        int diagCount1 = 0, diagCount2 = 0;
        for (int i = 0; i < 3; i++) {
            if (localBoardStatus[i*3 + i] == player) diagCount1++;
            if (localBoardStatus[i*3 + (2-i)] == player) diagCount2++;
        }
        if (diagCount1 == 2) score += 3; // Bonus pour diagonale
        if (diagCount2 == 2) score += 3; // Bonus pour diagonale

        return score;
    }

    /**
     * Évalue l'impact d'un coup forcé vers un plateau particulier
     */
    private static int evaluateForcedMove(int[] localBoardStatus, int nextLocalBoard,
                                          int playerLocalWins, int opponentLocalWins) {
        int score = 0;

        // Où se trouve le prochain plateau ?
        int nextRow = nextLocalBoard / 3;
        int nextCol = nextLocalBoard % 3;
        int nextBoardWeight = BOARD_WEIGHTS[nextRow][nextCol];

        // Si prochain plateau est déjà gagné/perdu, c'est très mauvais
        if (localBoardStatus[nextLocalBoard] != 0) {
            return -350; // Coup qui envoie vers un plateau terminé = mauvais
        }

        // Cas spécial: si l'adversaire a 2 plateaux gagnés et le prochain plateau
        // pourrait compléter une ligne, c'est une catastrophe
        if (opponentLocalWins >= 2) {
            if (couldCompleteWinningLine(localBoardStatus, nextLocalBoard, opponentLocalWins)) {
                return -800; // Presque une défaite certaine
            }
        }

        // Si le prochain plateau est stratégique mais pas gagné
        if (nextBoardWeight >= 3) {
            score -= nextBoardWeight * 40; // Moins pénalisant qu'avant
        } else {
            score -= nextBoardWeight * 20;
        }

        // Bonus si on est envoyé dans un plateau où on peut gagner
        int potentialToWin = evaluatePotentialToWin(localBoardStatus, nextLocalBoard);
        if (potentialToWin > 0) {
            score += potentialToWin * 30;
        }

        return score;
    }

    /**
     * Évalue si un plateau pourrait compléter une ligne gagnante
     */
    private static boolean couldCompleteWinningLine(int[] localBoardStatus, int boardIndex, int value) {
        int row = boardIndex / 3;
        int col = boardIndex % 3;

        // Vérifier la ligne
        int rowWins = 0;
        for (int j = 0; j < 3; j++) {
            if (localBoardStatus[row*3 + j] == value) rowWins++;
        }
        if (rowWins == 2) return true;

        // Vérifier la colonne
        int colWins = 0;
        for (int i = 0; i < 3; i++) {
            if (localBoardStatus[i*3 + col] == value) colWins++;
        }
        if (colWins == 2) return true;

        // Vérifier diagonale 1
        if (row == col) {
            int diagWins = 0;
            for (int i = 0; i < 3; i++) {
                if (localBoardStatus[i*3 + i] == value) diagWins++;
            }
            if (diagWins == 2) return true;
        }

        // Vérifier diagonale 2
        if (row + col == 2) {
            int diagWins = 0;
            for (int i = 0; i < 3; i++) {
                if (localBoardStatus[i*3 + (2-i)] == value) diagWins++;
            }
            if (diagWins == 2) return true;
        }

        return false;
    }

    /**
     * Évalue le potentiel de victoire dans un plateau local
     */
    private static int evaluatePotentialToWin(int[] localBoardStatus, int boardIndex) {
        // Cette fonction pourrait être étendue avec une analyse plus détaillée
        // Pour l'instant, on utilise simplement le poids stratégique du plateau
        int row = boardIndex / 3;
        int col = boardIndex % 3;
        return BOARD_WEIGHTS[row][col];
    }

    // Évaluation détaillée d'un plateau local ouvert - optimisée
    private static int evaluateLocalBoard(int[][] boardState, int startRow, int startCol,
                                          int player, int opponent, int boardWeight) {
        int score = 0;

        // Compte pièces et victoires potentielles
        int playerCount = 0;
        int opponentCount = 0;

        // Compte pièces dans ce plateau local
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int row = startRow + i;
                int col = startCol + j;
                int cellValue = boardState[row][col];
                int positionWeight = POSITION_WEIGHTS[i][j];

                if (cellValue == player) {
                    playerCount++;
                    score += positionWeight * 3;
                } else if (cellValue == opponent) {
                    opponentCount++;
                    score -= positionWeight * 4; // Défense prioritaire
                }
            }
        }

        // Le contrôle du centre est encore plus important pour Minimax
        if (boardState[startRow + 1][startCol + 1] == player) {
            score += CENTER_SCORE * 3 * boardWeight; // Renforcé
        } else if (boardState[startRow + 1][startCol + 1] == opponent) {
            score -= CENTER_SCORE * 4 * boardWeight; // Pénalité accrue
        } else {
            // Centre vide est un potentiel - valeur accrue
            score += boardWeight * 2;
        }

        // Évalue lignes, colonnes et diagonales - optimisé
        score += evaluateLocalLines(boardState, startRow, startCol, player, opponent, boardWeight);

        // Évalue les fourchettes - crucial pour Minimax
        int forkScore = evaluateForks(boardState, startRow, startCol, player, opponent);
        score += forkScore * (boardWeight / 2 + 1); // Pondération par importance du plateau

        return score;
    }

    private static int evaluateForks(int[][] boardState, int startRow, int startCol, int player, int opponent) {
        int score = 0;

        // Use an array to store counters that will be modified in lambdas
        final int[] counts = new int[4]; // [playerWinningPaths, playerThreatLines, opponentWinningPaths, opponentThreatLines]

        // Analyse optimisée des lignes
        for (int i = 0; i < 3; i++) {
            int[] rowCounts = countPieces(boardState, startRow + i, startCol, 0, 1, 3);
            int[] colCounts = countPieces(boardState, startRow, startCol + i, 1, 0, 3);

            // Évalue les menaces sur les lignes
            analyseThreatLine(rowCounts, player, opponent, ref -> {
                if (ref[0] == 1) counts[0]++;  // playerWinningPaths
                if (ref[0] == 2) counts[1]++;  // playerThreatLines
                if (ref[1] == 1) counts[2]++;  // opponentWinningPaths
                if (ref[1] == 2) counts[3]++;  // opponentThreatLines
            });

            // Évalue les menaces sur les colonnes
            analyseThreatLine(colCounts, player, opponent, ref -> {
                if (ref[0] == 1) counts[0]++;
                if (ref[0] == 2) counts[1]++;
                if (ref[1] == 1) counts[2]++;
                if (ref[1] == 2) counts[3]++;
            });
        }

        // Analyse les diagonales
        int[] diag1Counts = countPieces(boardState, startRow, startCol, 1, 1, 3);
        int[] diag2Counts = countPieces(boardState, startRow, startCol + 2, 1, -1, 3);

        // Évalue les menaces sur les diagonales
        analyseThreatLine(diag1Counts, player, opponent, ref -> {
            if (ref[0] == 1) counts[0]++;
            if (ref[0] == 2) counts[1]++;
            if (ref[1] == 1) counts[2]++;
            if (ref[1] == 2) counts[3]++;
        });

        analyseThreatLine(diag2Counts, player, opponent, ref -> {
            if (ref[0] == 1) counts[0]++;
            if (ref[0] == 2) counts[1]++;
            if (ref[1] == 1) counts[2]++;
            if (ref[1] == 2) counts[3]++;
        });

        // Score pour les fourchettes potentielles et actives
        if (counts[0] >= 2) {  // playerWinningPaths
            score += counts[0] * 60; // Valeur accrue
        }

        if (counts[2] >= 2) {  // opponentWinningPaths
            score -= counts[2] * 75; // Défense prioritaire
        }

        // Valeur des menaces immédiates (2 pièces dans une ligne)
        score += counts[1] * 25;  // playerThreatLines
        score -= counts[3] * 35;  // opponentThreatLines

        return score;
    }

    // Interface fonctionnelle pour callback
    private interface ThreatAnalysisCallback {
        void process(int[] result);
    }

    // Analyse une ligne pour les menaces
    private static void analyseThreatLine(int[] counts, int player, int opponent, ThreatAnalysisCallback callback) {
        int playerCount = counts[0];
        int opponentCount = counts[1];
        int emptyCount = counts[2];
        int[] result = new int[2];

        // Chemin gagnant pour joueur
        if (playerCount > 0 && opponentCount == 0 && emptyCount > 0) {
            if (playerCount == 1) result[0] = 1;      // Chemin potentiel
            else if (playerCount == 2) result[0] = 2; // Menace immédiate
        }

        // Chemin gagnant pour adversaire
        if (opponentCount > 0 && playerCount == 0 && emptyCount > 0) {
            if (opponentCount == 1) result[1] = 1;      // Chemin potentiel
            else if (opponentCount == 2) result[1] = 2; // Menace immédiate
        }

        callback.process(result);
    }

    // Compte les pièces sur une ligne/colonne/diagonale
    private static int[] countPieces(int[][] board, int startRow, int startCol,
                                     int rowDelta, int colDelta, int length) {
        int playerCount = 0;
        int opponentCount = 0;
        int emptyCount = 0;

        for (int i = 0; i < length; i++) {
            int row = startRow + i * rowDelta;
            int col = startCol + i * colDelta;
            int cell = board[row][col];

            if (cell == 4 || cell == 5) playerCount++;
            else if (cell == 2 || cell == 3) opponentCount++;
            else emptyCount++;
        }

        return new int[] {playerCount, opponentCount, emptyCount};
    }

    // Évalue lignes, colonnes et diagonales d'un plateau local - optimisé
    private static int evaluateLocalLines(int[][] boardState, int startRow, int startCol,
                                          int player, int opponent, int boardWeight) {
        int score = 0;

        // Variables pour optimiser la recherche de menaces
        boolean playerHasTwoInRow = false;
        boolean opponentHasTwoInRow = false;

        // Évalue les lignes et colonnes avec une méthode plus optimisée
        for (int i = 0; i < 3; i++) {
            int[] rowResult = evaluateLineSegment(boardState, startRow + i, startCol, 0, 1, 3, player, opponent);
            int[] colResult = evaluateLineSegment(boardState, startRow, startCol + i, 1, 0, 3, player, opponent);

            score += rowResult[0] * boardWeight;
            score += colResult[0] * boardWeight;

            // Détecte les menaces immédiates
            if (rowResult[1] > 0 || colResult[1] > 0) playerHasTwoInRow = true;
            if (rowResult[2] > 0 || colResult[2] > 0) opponentHasTwoInRow = true;
        }

        // Évalue les diagonales avec la même méthode optimisée
        int[] diag1Result = evaluateLineSegment(boardState, startRow, startCol, 1, 1, 3, player, opponent);
        int[] diag2Result = evaluateLineSegment(boardState, startRow, startCol + 2, 1, -1, 3, player, opponent);

        // Les diagonales restent légèrement plus valorisées
        score += diag1Result[0] * boardWeight * 1.3;
        score += diag2Result[0] * boardWeight * 1.3;

        // Détecte les menaces immédiates sur les diagonales
        if (diag1Result[1] > 0 || diag2Result[1] > 0) playerHasTwoInRow = true;
        if (diag1Result[2] > 0 || diag2Result[2] > 0) opponentHasTwoInRow = true;

        // Bonus supplémentaire pour menaces immédiates
        if (playerHasTwoInRow) {
            score += TWO_IN_A_ROW_SCORE * boardWeight / 2;
        }

        if (opponentHasTwoInRow) {
            score -= TWO_IN_A_ROW_SCORE * boardWeight * 0.7;
        }

        return score;
    }

    // Évalue une ligne/colonne/diagonale avec optimisations
    private static int[] evaluateLineSegment(int[][] board, int startRow, int startCol,
                                             int rowDelta, int colDelta, int length,
                                             int player, int opponent) {
        int playerCount = 0;
        int opponentCount = 0;
        int emptyCount = 0;

        for (int i = 0; i < length; i++) {
            int row = startRow + i * rowDelta;
            int col = startCol + i * colDelta;
            int cell = board[row][col];

            if (cell == player) playerCount++;
            else if (cell == opponent) opponentCount++;
            else emptyCount++;
        }

        int score = 0;
        int playerTwoInRow = 0;
        int opponentTwoInRow = 0;

        // Si seulement pièces du joueur dans la ligne
        if (playerCount > 0 && opponentCount == 0) {
            if (playerCount == 1) {
                score += 1;
            } else if (playerCount == 2 && emptyCount == 1) {
                score += TWO_IN_A_ROW_SCORE; // Deux pièces avec la troisième vide
                playerTwoInRow = 1;
            }
        }

        // Si seulement pièces de l'adversaire
        if (opponentCount > 0 && playerCount == 0) {
            if (opponentCount == 1) {
                score -= 1;
            } else if (opponentCount == 2 && emptyCount == 1) {
                // Pénalité pour adversaire sur le point de gagner
                score -= TWO_IN_A_ROW_SCORE * 1.5;
                opponentTwoInRow = 1;
            }
        }

        return new int[] {score, playerTwoInRow, opponentTwoInRow};
    }

    // Évalue motifs globaux - optimisé
    private static int evaluateGlobalPatterns(int[] localBoardStatus, int player, int opponent) {
        int score = 0;
        boolean playerHasThreat = false;
        boolean opponentHasThreat = false;

        // Évalue lignes
        for (int i = 0; i < 3; i++) {
            int[] rowAnalysis = analyzeGlobalLine(localBoardStatus, i * 3, 1, player, opponent);
            score += rowAnalysis[0];
            if (rowAnalysis[1] > 0) playerHasThreat = true;
            if (rowAnalysis[2] > 0) opponentHasThreat = true;
        }

        // Évalue colonnes
        for (int j = 0; j < 3; j++) {
            int[] colAnalysis = analyzeGlobalLine(localBoardStatus, j, 3, player, opponent);
            score += colAnalysis[0];
            if (colAnalysis[1] > 0) playerHasThreat = true;
            if (colAnalysis[2] > 0) opponentHasThreat = true;
        }

        // Diagonale (haut-gauche vers bas-droite)
        int[] diag1Analysis = analyzeGlobalDiag(localBoardStatus, 0, 4, player, opponent);
        score += diag1Analysis[0] * 1.5; // Bonus pour diagonale
        if (diag1Analysis[1] > 0) playerHasThreat = true;
        if (diag1Analysis[2] > 0) opponentHasThreat = true;

        // Diagonale (haut-droite vers bas-gauche)
        int[] diag2Analysis = analyzeGlobalDiag(localBoardStatus, 2, 2, player, opponent);
        score += diag2Analysis[0] * 1.5; // Bonus pour diagonale
        if (diag2Analysis[1] > 0) playerHasThreat = true;
        if (diag2Analysis[2] > 0) opponentHasThreat = true;

        // Bonus/Malus supplémentaire pour les menaces globales
        if (playerHasThreat) {
            score += POTENTIAL_WIN_SCORE / 3;
        }

        if (opponentHasThreat) {
            score -= POTENTIAL_WIN_SCORE / 2; // Défense prioritaire
        }

        return score;
    }

    // Analyse une ligne au niveau global
    private static int[] analyzeGlobalLine(int[] status, int start, int step, int player, int opponent) {
        int playerCount = 0;
        int opponentCount = 0;
        int emptyCount = 0;

        for (int i = 0; i < 3; i++) {
            int idx = start + i * step;
            if (status[idx] == player) playerCount++;
            else if (status[idx] == opponent) opponentCount++;
            else if (status[idx] == 0) emptyCount++;
        }

        int score = 0;
        int playerThreat = 0;
        int opponentThreat = 0;

        // Si seulement des victoires du joueur dans la ligne
        if (playerCount > 0 && opponentCount == 0) {
            if (playerCount == 1 && emptyCount == 2) {
                score += 75; // Une victoire avec deux plateaux potentiels
            } else if (playerCount == 2 && emptyCount == 1) {
                score += POTENTIAL_WIN_SCORE; // Deux victoires avec une de plus pour gagner
                playerThreat = 1;
            }
        }

        // Si seulement des victoires de l'adversaire
        if (opponentCount > 0 && playerCount == 0) {
            if (opponentCount == 1 && emptyCount == 2) {
                score -= 100; // Bloquer la progression adverse
            } else if (opponentCount == 2 && emptyCount == 1) {
                score -= POTENTIAL_WIN_SCORE * 1.2; // Bloquer la victoire adverse
                opponentThreat = 1;
            }
        }

        return new int[] {score, playerThreat, opponentThreat};
    }

    // Analyse une diagonale au niveau global
    private static int[] analyzeGlobalDiag(int[] status, int start, int step, int player, int opponent) {
        int playerCount = 0;
        int opponentCount = 0;
        int emptyCount = 0;

        for (int i = 0; i < 3; i++) {
            int idx = start + i * step;
            if (status[idx] == player) playerCount++;
            else if (status[idx] == opponent) opponentCount++;
            else if (status[idx] == 0) emptyCount++;
        }

        int score = 0;
        int playerThreat = 0;
        int opponentThreat = 0;

        // Si seulement des victoires du joueur
        if (playerCount > 0 && opponentCount == 0) {
            if (playerCount == 1 && emptyCount == 2) {
                score += 90; // Diagonale plus valorisée
            } else if (playerCount == 2 && emptyCount == 1) {
                score += POTENTIAL_WIN_SCORE * 1.2; // Diagonale plus valorisée
                playerThreat = 1;
            }
        }

        // Si seulement des victoires de l'adversaire
        if (opponentCount > 0 && playerCount == 0) {
            if (opponentCount == 1 && emptyCount == 2) {
                score -= 120; // Bloquer la progression adverse
            } else if (opponentCount == 2 && emptyCount == 1) {
                score -= POTENTIAL_WIN_SCORE * 1.4; // Bloquer la victoire adverse
                opponentThreat = 1;
            }
        }

        return new int[] {score, playerThreat, opponentThreat};
    }
}