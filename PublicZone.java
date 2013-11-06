import java.io.*;
import javax.jms.*;
import javax.naming.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;
import java.lang.StringBuffer;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;
import org.objectweb.joram.client.jms.tcp.TopicTcpConnectionFactory;
import org.objectweb.joram.client.jms.tcp.QueueTcpConnectionFactory;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.Topic;
import javax.jms.JMSException;

class PublicZone {
    private TopicSession ts;
    private QueueConnection qc;
    private QueueReceiver publication;
    private QueueReceiver winner;
    private TopicPublisher update;
    private TopicPublisher top10;
    private  List<Player> players;
    private  List<Announcement> announcements;
    public PublicZone() throws Exception {
        players = Collections.synchronizedList(new ArrayList<Player>());
        announcements = Collections.synchronizedList(new ArrayList<Announcement>());
        //At boot the admin object is created
        AdminModule.connect("root", "root", 60);
        javax.jms.ConnectionFactory cf = TcpConnectionFactory.create("localhost", 16010);
        javax.jms.QueueConnectionFactory qcf = QueueTcpConnectionFactory.create("localhost", 16010);
        javax.jms.TopicConnectionFactory tcf = TopicTcpConnectionFactory.create("localhost", 16010);
        javax.naming.Context jndiCtx = new javax.naming.InitialContext();
        AdminModule.disconnect();
        try{
            jndiCtx.bind("cf", cf);
        }
        catch(javax.naming.NameAlreadyBoundException e) {
            System.out.println("cf element already bound");
        }
        try {
            jndiCtx.bind("qcf", qcf);
        }
        catch(javax.naming.NameAlreadyBoundException e) {
            System.out.println("qcf element already bound");
        }
        try {
            jndiCtx.bind("tcf", tcf);
        }
        catch(javax.naming.NameAlreadyBoundException e) {
            System.out.println("tcf element already bound");
        }
        jndiCtx.close();
        AdminModule.disconnect();
        //Adding the queue to receive the publication from user
        try{
            addQueue("Publication");
        }
        catch(javax.naming.NameAlreadyBoundException e) {
            System.out.println("Publication queue already present");
        }
        Context ictx = new InitialContext();
        Queue queue = (Queue) ictx.lookup("Publication");
        qc = qcf.createQueueConnection();
        QueueSession qs = qc.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        publication = qs.createReceiver(queue);
        runPublicationThread();
        //Adding the queue to receive the winner from a match
        try {
            addQueue("Winner");
        }
        catch(javax.naming.NameAlreadyBoundException e) {
            System.out.println("Winner queue already present");
        }
        Queue win = (Queue) ictx.lookup("Winner");
        QueueSession winnerSession = qc.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        winner = winnerSession.createReceiver(win);
        //Registration of the topic update
        try {
            addTopic("Update");
        }
        catch (javax.naming.NameAlreadyBoundException e) {
            System.out.println("Update topic already present");
        }
        Topic topic = (Topic) ictx.lookup("Update");
        TopicConnection tc = tcf.createTopicConnection();
        ts = tc.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
        update = ts.createPublisher(topic);
        runUpdateToAllThread();
        //Registration of the topic top10
        try {
            addTopic("Top10");
        }
        catch (javax.naming.NameAlreadyBoundException e) {
            System.out.println("Top10 topic already present");
        }
        Topic top = (Topic) ictx.lookup("Top10");
        top10 = ts.createPublisher(top);
        ictx.close();
        qc.start();
        runTop10Thread();
    }

    public void runTop10Thread() {
        Thread t = new Thread(new Runnable(){
            public void run() {
                //Body of the runTop10Thread
                TextMessage msg = null;
                while(true) {
                    try {
                        synchronized(winner) {
                        msg = (TextMessage)  winner.receive(10000);
                        }
                    }
                    catch (JMSException e) {
                        e.printStackTrace();
                    }
                    if (msg == null) {
                        //print 10 element
                        synchronized(players) {
                            Iterator i = players.iterator();
                            Player[] toSend = new Player[10];
                            for (int j =0; j<10; j++) {
                                if (i.hasNext()) {
                                    toSend[j] = (Player)i.next();
                                }
                                else {
                                    toSend[j] = null;
                                }
                            }
                            String toSendS = serialize(toSend);
                            System.out.println(toSendS);
                            try {
                                TextMessage toSendTextMessage = ts.createTextMessage();
                                toSendTextMessage.setText(toSendS);
                                top10.publish(toSendTextMessage);
                                ts.commit();
                            }
                            catch(JMSException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        String[] tokens= null;
                        try{
                            tokens = msg.getText().split(",");
                        }
                        catch (JMSException e) {
                            e.printStackTrace();
                        }
                        String winner = tokens[0];
                        String other = tokens[1];
                        synchronized(players) {
                            Player toModify1 = null;
                            Player toModify2 = null;
                            boolean modified1 = false;
                            boolean modified2 = false;
                            for (Player el : players) {
                                if (el.getName().compareTo(winner) == 0) {
                                    toModify1 = el;
                                    modified1 = true;
                                }
                                else if (el.getName().compareTo(other) == 0) {
                                    toModify2 = el;
                                    modified2 = true;
                                }
                            }
                            if (toModify1 == null) {
                                toModify1 = new Player(winner, new Integer(1));
                            }
                            if (toModify2 == null) {
                                toModify2 = new Player(other, new Integer(-1));
                            }
                            if (modified1) {
                                players.remove(toModify1);
                                toModify1.setPoints(toModify1.getPoints()+1);
                                players.add(toModify1);
                            }
                            else {
                                players.add(toModify1);
                            }
                            if (modified2) {
                                players.remove(toModify2);
                                toModify2.setPoints(toModify1.getPoints()+1);
                                players.add(toModify2);
                            }
                            else {
                                players.add(toModify2);
                            }
                            Collections.sort(players, new CustomComparator());
                        }
                    }
                }
            }
        });
        t.start();
    }
    public void runUpdateToAllThread() {
        Thread t = new Thread(new Runnable(){
            public void run() {
                while(true) {
                    synchronized (announcements) {
                        String toString = serialize(announcements);
                        try {
                            TextMessage msg = ts.createTextMessage();
                            msg.setText(toString);
                            update.publish(msg);
                            ts.commit();
                        }
                        catch(JMSException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(10000);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }
    public void runPublicationThread() {
        Thread t = new Thread(new Runnable(){
            public void run() {
                while (true) {
                    TextMessage msg = null;
                    try {
                        msg = (TextMessage) publication.receive();
                    }
                    catch (JMSException e) {
                        e.printStackTrace();
                    }
                    synchronized (announcements) {
                        String[] tokens= null;
                        try{
                            tokens = msg.getText().split(",");
                        }
                        catch (JMSException e) {
                            e.printStackTrace();
                        }
                        String owner = tokens[0];
                        String nameChannel = tokens[1];
                        if (nameChannel.compareTo("-1") == 0) {
                            for (int i =0; i<announcements.size(); i++) {
                                if (announcements.get(i).getOwner().compareTo(owner) == 0) {
                                    announcements.remove(announcements.get(i));
                                }
                            }
                            return;
                        }
                        boolean found = false;
                        for (Announcement el :  announcements) {
                            if (el.getOwner().compareTo(owner) == 0 && el.getNameChannel().compareTo(nameChannel) == 0) {
                                found = true;
                            }
                        }
                        if (!found) {
                            announcements.add(new Announcement(owner, nameChannel));
                        }
                        String toString = serialize(announcements);
                        try {
                            msg = ts.createTextMessage();
                            msg.setText(toString);
                            update.publish(msg);
                            ts.commit();
                        }
                        catch (JMSException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        t.start();
    }
    // This method allows to create a queue inside the broker
    public void addQueue (String queueName) throws Exception{
        try {
            AdminModule.connect("root", "root", 60);
        }
        catch (java.net.ConnectException e) {
            System.out.println("Already connect to AdminModule");
        }
        Queue queue = Queue.create(queueName);
        User.create("anonymous", "anonymous");
        queue.setFreeReading();
        queue.setFreeWriting();
        javax.naming.Context jndiCtx = new javax.naming.InitialContext();
        jndiCtx.bind(queueName, queue);
        jndiCtx.close();
        AdminModule.disconnect();
    }
    // This method allows to create a topic inside the broker
    public void addTopic (String topicName) throws Exception {
        try {
            AdminModule.connect("root", "root", 60);
        }
        catch(java.net.ConnectException e) {
            System.out.println("Already connect to AdminModule");
        }
        Topic topic = Topic.create(topicName);
        User.create("anonymous", "anonymous");
        topic.setFreeReading();
        topic.setFreeWriting();
        javax.naming.Context jndiCtx = new javax.naming.InitialContext();
        jndiCtx.bind(topicName, topic);
        jndiCtx.close();
        AdminModule.disconnect();
    }
    public String serialize (Player[] p) {
        synchronized(p) {
            StringBuffer buf =  new StringBuffer();
            boolean first = true;
            for (Player el : p) {
                if (el != null && el.getName() != null && el.getPoints()!=null){
                    if (first) {
                        buf.append(el.getName()+","+el.getPoints());
                        first = false;
                    }
                    else {
                        buf.append(";"+el.getName()+","+el.getPoints());
                    }
                }
                else {
                    if (first) {
                        buf.append("NULL,NULL");
                        first = false;
                    }
                    else {
                        buf.append(";NULL,NULL");
                    }
                }
            }
            return buf.toString();
        }
    }
    public String serialize (List<Announcement> a) {
        synchronized(a) {
            StringBuffer buf = new StringBuffer();
            boolean first = true;
            for (Announcement el : a) {
                if (el != null) {
                    if (first) {
                        buf.append(el.getOwner()+","+el.getNameChannel());
                        first = false;
                    }
                    else {
                        buf.append(";"+el.getOwner()+","+el.getNameChannel());
                    }
                }
                else {
                    if (first) {
                        buf.append("NULL,NULL");
                        first = false;
                    }
                    else {
                        buf.append(";NULL,NULL");
                    }
                }
            }
            if (buf.length() == 0) {
                buf.append("NULL,NULL");
            }
            return buf.toString();
        }
    }
    public static void main(String[] args) {
        try {
            PublicZone my_public =  new PublicZone();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
