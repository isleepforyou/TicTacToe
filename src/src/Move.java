class Move {
    private int globalIndex; // Which local board (0-8)
    private int localIndex;  // Which cell within the local board (0-8)

    public Move() {
        globalIndex = -1;
        localIndex = -1;
    }

    public Move(int globalIndex, int localIndex) {
        this.globalIndex = globalIndex;
        this.localIndex = localIndex;
    }

    public int getGlobalIndex() {
        return globalIndex;
    }

    public int getLocalIndex() {
        return localIndex;
    }

    @Override
    public String toString() {
        return "Global: " + globalIndex + ", Local: " + localIndex;
    }
}