/**
 * Représente un coup dans le jeu Ultimate Tic-Tac-Toe
 */
public class Move {
    private int row;
    private int col;

    public Move(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public String toString() {
        return "Move(" + row + ", " + col + ")";
    }
}