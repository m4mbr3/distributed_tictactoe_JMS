class Winner {
    private String winner;
    private String other;
    private Boolean pair;

    public Winner(String winner, String other, Boolean pair) {
        this.winner =  winner;
        this.other = other;
        this.pair = pair;
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
    public void setPair (Boolean pair) {
        this.pair = pair;
    }
    public Boolean getPair () {
        return this.pair;
    }
}
