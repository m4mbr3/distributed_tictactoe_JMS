import java.util.Collections;
import java.io.Console;
import java.util.List;
import java.util.ArrayList;
import java.lang.Integer;
import javax.jms.*;
import javax.naming.*;

class User {
    private List<Player> top10;
    User () {
        top10 = Collections.synchronizedList(new ArrayList<Player>());
        runOffersList();
    }
    static Context ictx = null;

    public static void menu() {
        System.out.print("\033[H\033[2J");
        System.out.flush();

        System.out.println ("/************************* TicTacToe ***************************/");
        System.out.println ("/*********** Author : Andrea Mambretti Version 1.0 *************/ ");
        System.out.println ("/********************************************************************/ ");
        System.out.println ("Select an operation:");
        System.out.println ("");
        System.out.println (" publish_match");
        System.out.println (" show_match");
        System.out.println (" answer_match");
        System.out.println (" clear");
        System.out.println (" help");
        System.out.println (" exit");
        System.out.println ("");
    }

    public void runOffersList(){
        try {
            ictx = new InitialContext();
            Topic topic = (Topic) ictx.lookup("Top10");
            TopicConnectionFactory tcf = (TopicConnectionFactory) ictx.lookup("tcf");
            ictx.close();
            TopicConnection tc = tcf.createTopicConnection();
            TopicSession ts = tc.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
            TopicSubscriber tsub = ts.createSubscriber(topic);
            MsgListener msg =  new MsgListener(top10);
            tsub.setMessageListener(msg);
            tc.start();
        }
        catch (javax.naming.NamingException e) {
            e.printStackTrace();
        }
        catch (JMSException e) {
            e.printStackTrace();
        }

    }

    public void printTop10() {
        synchronized(top10) {
            System.out.println("*********************TOP10*******************");
            System.out.println("  Username   |   Points");
            int i = 0;
            for (Player el : top10 ) {
                i++;
                String name = null;
                try {
                    name = el.getName().substring(0,11);
                }
                catch (IndexOutOfBoundsException e) {
                    int size =  el.getName().length();
                    for(int j=0; j<(11-size); j++ ) {
                        el.setName(el.getName()+" ");
                    }
                }
                System.out.println(i+")"+el.getName()+"|   "+el.getPoints());
            }
            System.out.println("*********************************************");
        }
    }
    public static void main(String[] args) {
        User u = new User();
        Console con = System.console();
        String user_name = con.readLine("Insert your username for this session: ");
        String c="";
        menu ();
        while (c.compareTo("exit") != 0) {
            c = con.readLine(user_name+"@tictactoe $ ").trim();
            if (c.compareTo("publish_match") == 0) {
                publish_match();
            }
            else if (c.compareTo("top10") == 0) {
                show_top10(u);
            }
            else if (c.compareTo("show_match") == 0) {
                //show_match();
            }
            else if (c.compareTo("answer_match") == 0) {
                //answer_match();
            }
            else if (c.compareTo("clear") == 0) {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
            else if (c.compareTo("help") == 0) {
                menu();
            }
            else if (c.compareTo("exit") != 0) {
                System.out.println("dropboxlike: "+ c + ": command not found");
            }
        }
    }
    static void show_top10(User u) {
        u.printTop10();
    }
    static void publish_match () {

    }

}

class MsgListener implements MessageListener {
    String id;
    List<Player> top10;
    public MsgListener(List<Player> top10) {
        this.top10 = top10;
    }
    public MsgListener() {id = "";}
    public MsgListener(String id) {this.id = id;}
    public void onMessage (Message msg) {
        TextMessage tmsg =  (TextMessage) msg;
        try {
            String[] tokens = null;
            tokens = tmsg.getText().split(";");
            synchronized (top10){
                top10.clear();
                for (int i = 0; i < tokens.length; i++) {
                    String[] info = null;
                    info = tokens[i].split(",");
                    String name =  info[0];
                    String p = info[1];
                    if (p.compareTo("NULL") == 0 || name.compareTo("NULL") == 0)  {
                        top10.add(new Player("Empty"));
                    }
                    else {
                        Integer points = new Integer(Integer.parseInt(p));
                        top10.add(new Player(name, points));
                    }
                }
            }
        }
        catch (JMSException  e) {
            e.printStackTrace();
        }
    }
}

