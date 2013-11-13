class Player {
    private String name;
    private Integer points;

    Player (String name) {
        this.name = new String(name);
        points = new Integer(0);
    }
    Player (String name, Integer points) {
        this.name = new String(name);
        this.points = new Integer(points);
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


