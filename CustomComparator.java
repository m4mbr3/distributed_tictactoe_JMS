import java.util.Comparator;

public class CustomComparator implements Comparator<Player> {
    @Override
    public int compare(Player p1, Player p2) {
        return p2.getPoints() - p1.getPoints();
    }
}
