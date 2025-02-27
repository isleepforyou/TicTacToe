# Ultimate Tic-Tac-Toe - Client IA

Ce projet est une implémentation du jeu Ultimate Tic-Tac-Toe avec une intelligence artificielle basée sur l'algorithme Minimax Alpha-Beta.

## Description du jeu

L'Ultimate Tic-Tac-Toe est une version avancée du jeu classique. Le plateau est composé de 9 grilles de tic-tac-toe, formant une grande grille 3x3. Pour gagner, un joueur doit gagner 3 sous-grilles alignées.

La particularité: le coup joué détermine dans quelle grille l'adversaire doit jouer son prochain coup.

## Structure du projet

- `Board.java` - Représentation du plateau de jeu
- `Client.java` - Client pour se connecter à l'interface graphique du jeu
- `Evaluator.java` - Évalue la position du jeu
- `MinimaxAlphaBeta.java` - Implémentation de l'algorithme Minimax Alpha-Beta
- `Move.java` - Représente un coup
- `MoveGenerator.java` - Génère les coups valides

## Compilation

```bash
javac *.java
```

## Exécution

```bash
java Client [adresse_serveur] [port]
```

Par défaut:
- Adresse: localhost
- Port: 8888

## Fonctionnement de l'IA

L'IA utilise:
- Algorithme Minimax avec Alpha-Beta
- Approfondissement itératif (augmente progressivement la profondeur d'analyse)
- Fonction d'évaluation prenant en compte:
    - Contrôle des positions stratégiques (centre, coins)
    - Menaces immédiates
    - Possibilités de fourchettes
    - Valeur stratégique des plateaux locaux

## Particularités techniques

- Temps limite de réflexion: 2.8 secondes par coup
- Profondeur maximale d'analyse: 12 niveaux

## Format de communication

Le client communique avec le serveur par:
- Lecture des commandes (caractères '1' à '5')
- Envoi des coups au format lettre-chiffre (ex: "D6")

## Auteurs

Mika Cuvillier 

Jérémy Emond-Lapierre

Alexis Boutin

