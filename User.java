import java.util.Collections;
import java.io.Console;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Integer;
import javax.jms.*;
import javax.naming.*;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;
import org.objectweb.joram.client.jms.tcp.TopicTcpConnectionFactory;
import org.objectweb.joram.client.jms.tcp.QueueTcpConnectionFactory;
import org.objectweb.joram.client.jms.admin.AdminModule;
//import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.Topic;

class User {
    private List<Player> top10;
    private List<Announcement> announcements;
    private TopicConnection tc;
    private QueueConnection qc;
    private QueueSender publicator;

    User () {
        top10 = Collections.synchronizedList(new ArrayList<Player>());
        announcements = Collections.synchronizedList(new ArrayList<Announcement>());
        qc = null;
        runOffersList();
    }
    static Context ictx = null;

    public static void menu() {
        System.out.print("\033[H\033[2J");
        System.out.flush();

        System.out.println ("/************************* TicTacToe ***************************/");
        System.out.println ("/*********** Author : Andrea Mambretti Version 1.0 *************/ ");
        System.out.println ("/***************************************************************/ ");
        System.out.println ("Select an operation:");
        System.out.println ("");
        System.out.println (" publish_match");
        System.out.println (" show_match");
        System.out.println (" answer_match");
        System.out.println (" top10");
        System.out.println (" clear");
        System.out.println (" help");
        System.out.println (" exit");
        System.out.println ("");
    }

    public void runOffersList(){
        try {
            //Creation session and factory object
            ictx = new InitialContext();
            TopicConnectionFactory tcf = (TopicConnectionFactory) ictx.lookup("tcf");
            QueueConnectionFactory qcf = (QueueConnectionFactory) ictx.lookup("qcf");
            //Definition of the Topic/Queue globally used
            Topic topic = (Topic) ictx.lookup("Top10");
            Topic topic2 = (Topic) ictx.lookup("Update");
            Queue publicationQueue = (Queue) ictx.lookup("Publication");
            //Creation of the connections
            qc = qcf.createQueueConnection();
            tc = tcf.createTopicConnection();
            //Creation of sessions
            TopicSession ts = tc.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
            QueueSession qs = qc.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            //Creation of subscriber/sender
            TopicSubscriber tsub = ts.createSubscriber(topic);
            TopicSubscriber tsub2 = ts.createSubscriber(topic2);
            publicator = qs.createSender(publicationQueue);
            tsub.setMessageListener(new MsgListenerTop10(top10));
            tsub2.setMessageListener(new MsgListenerUpdate(announcements));
            tc.start();
        }
        catch (javax.naming.NamingException e) {
            e.printStackTrace();
        }
        catch (JMSException e) {
            e.printStackTrace();
        }
    }
    public void game (QueueReceiver temprec) {
        TextMessage msg = null;
        try {
            msg = (TextMessage) temprec.receive();
        }
        catch (JMSException e) {
            e.printStackTrace();
        }

    }
    public void publish_match(String username) {
        Random m = new Random();
        Integer n = new Integer(m.nextInt()%10000);
        String toCreate = new String(username+n.toString());
        //Creation of the temporary queue to accept connection.
        try {
            addQueue(toCreate);
            Queue queue = (Queue) ictx.lookup(toCreate);
            QueueSession qs = qc.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueReceiver temprec = qs.createReceiver(queue);
            //Creation and send to the public zone of the banner
            TextMessage msg = qs.createTextMessage();
            msg.setText(username+","+toCreate);
            publicator.send(msg);
            game(temprec);
        }
        catch (javax.naming.NamingException e) {
            e.printStackTrace();
        }
        catch (JMSException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void addQueue (String queueName) throws Exception{
        try {
            AdminModule.connect("root", "root", 60);
        }
        catch (java.net.ConnectException e) {
            System.out.println("Already connect to AdminModule");
        }
        Queue queue = Queue.create(queueName);
        org.objectweb.joram.client.jms.admin.User.create("anonymous", "anonymous");
        queue.setFreeReading();
        queue.setFreeWriting();
        javax.naming.Context jndiCtx = new javax.naming.InitialContext();
        jndiCtx.bind(queueName, queue);
        jndiCtx.close();
        AdminModule.disconnect();
    }


    public void show_match() {
        synchronized(announcements) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
            System.out.println("******************ANNOUNCEMENTS****************");
            System.out.println("   Username   |   Port");
            for (Announcement el : announcements) {
            String name = null;
                try {
                    name = el.getOwner().substring(0,11);
                }
                catch (IndexOutOfBoundsException e) {
                    int size =  el.getOwner().length();
                    for(int j=0; j<(11-size); j++ ) {
                        el.setOwner(el.getOwner()+" ");
                    }
                }
                System.out.println("   "+el.getOwner()+"|   "+el.getNameChannel());
            }
            System.out.println("************************************************");
        }
    }
    public void stop() {
        try{
           // publicator.send
            tc.stop();
            tc.close();
            ictx.close();
            if (qc != null) qc.close();
        }
        catch (JMSException e) {
            e.printStackTrace();
        }
        catch (javax.naming.NamingException e) {
            e.printStackTrace();
        }
    }
    public void printTop10() {
        synchronized(top10) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
            System.out.println("*********************TOP10*******************");
            System.out.println("    Username |   Points");
            int i = 0;
            for (Player el : top10 ) {
                i++;
                String name = null;
                try {
                    name = el.getName().substring(0,9);
                }
                catch (IndexOutOfBoundsException e) {
                    int size =  el.getName().length();
                    for(int j=0; j<(9-size); j++ ) {
                        el.setName(el.getName()+" ");
                    }
                }
                if (i!=10)
                    System.out.println(i+" ) "+el.getName()+"|   "+el.getPoints());
                else
                    System.out.println(i+") "+el.getName()+"|   "+el.getPoints());
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
                publish_match(u,user_name);
            }
            else if (c.compareTo("top10") == 0) {
                show_top10(u);
            }
            else if (c.compareTo("show_match") == 0) {
                show_match(u);
            }
            else if (c.compareTo("answer_match") == 0) {
            //    answer_match();
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
        u.stop();
    }
    static void publish_match(User u, String username) {
        u.publish_match(username);
    }
    static void show_top10(User u) {
        u.printTop10();
    }
    static void show_match (User u) {
        u.show_match();
    }

}
class MsgListenerUpdate implements MessageListener {
    List<Announcement> announcements;
    MsgListenerUpdate(List<Announcement> announcements) {
        this.announcements = announcements;
    }
    public void onMessage(Message msg) {
        TextMessage tmsg = (TextMessage) msg;
        try {
            String[] tokens = null;
            tokens = tmsg.getText().split(";");
            synchronized (announcements) {
                announcements.clear();
                for (int i = 0; i < tokens.length; i++) {
                    String[] info = null;
                    info = tokens[i].split(",");
                    String owner = info[0];
                    String channel = info[1];
                    if (owner.compareTo("NULL") == 0 || channel.compareTo("NULL") == 0) {
                        announcements.add(new Announcement("Empty", "Empty"));
                    }
                    else {
                        announcements.add(new Announcement(owner, channel));
                    }
                }
            }
        }
        catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
class MsgListenerTop10 implements MessageListener {
    List<Player> top10;
    public MsgListenerTop10(List<Player> top10) {
        this.top10 = top10;
    }
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

