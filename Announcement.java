class Announcement {
    private String owner;
    private String nameChannel;

    Announcement (String owner, String nameChannel) {
        this.owner = new String(owner);
        this.nameChannel = new String(nameChannel);
    }
    public void setOwner (String owner) {
        this.owner = owner;
    }
    public String getOwner () {
        return this.owner;
    }
    public void setNameChannel (String nameChannel) {
        this.nameChannel = nameChannel;
    }
    public String  getNameChannel () {
        return this.nameChannel;
    }
}
