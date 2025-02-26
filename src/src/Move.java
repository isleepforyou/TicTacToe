class Move {
    private int row;
    private int col;

    public Move() {
        row = -1;
        col = -1;
    }

    public Move(int r, int c) {
        row = r;
        col = c;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setRow(int r) {
        row = r;
    }

    public void setCol(int c) {
        col = c;
    }

    // Méthodes utilitaires pour le tic-tac-toe géant

    // Obtenir le plateau local (0-2, 0-2)
    public int getGlobalRow() {
        return row / 3;
    }

    public int getGlobalCol() {
        return col / 3;
    }

    // Obtenir la position dans le plateau local (0-2, 0-2)
    public int getLocalRow() {
        return row % 3;
    }

    public int getLocalCol() {
        return col % 3;
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}