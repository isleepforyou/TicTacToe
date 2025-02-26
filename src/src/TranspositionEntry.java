class TranspositionEntry {
    private int score;
    private int depth;

    public TranspositionEntry(int score, int depth) {
        this.score = score;
        this.depth = depth;
    }

    public int getScore() {
        return score;
    }

    public int getDepth() {
        return depth;
    }
}