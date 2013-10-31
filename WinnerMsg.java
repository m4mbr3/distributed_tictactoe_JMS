class Winner {
    private String winner;
    private String other;

    public Winner(String winner, String other, Boolean pair) {
        this.winner =  winner;
        this.other = other;
    }
    public void setWinner (String winner) {
        this.winner = winner;
    }
    public String getWinner () {
        return this.winner;
    }
    public void setOther (String other) {
        this.other = other;
    }
    public String getOther () {
        return this.other;
    }
}
