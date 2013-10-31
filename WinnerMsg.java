class Winner {
    String winner;
    String other;
    int result;
    public Winner(String winner, String other, int result) {
        this.winner =  winner;
        this.other = other;
        this.result = result;
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
    public void setResult (int result) {
        this.result = result;
    }
    public int getResult () {
        return this.result;
    }
}
