class Player {
    private String name;
    private Integer points;

    Player (String name) {
        this.name = name;
        points = new Integer(0);
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getName () {
        return this.name;
    }
    public void setPoints(Integer points) {
        this.points = points;
    }
    public Integer getPoints() {
        return points;
    }
}
