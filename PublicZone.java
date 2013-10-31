import java.io.*;
import javax.jms.*;
import javax.naming.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;
import org.objectweb.joram.client.jms.tcp.TopicTcpConnectionFactory;
import org.objectweb.joram.client.jms.tcp.QueueTcpConnectionFactory;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.Topic;
import javax.jms.JMSException;

class PublicZone {
    private QueueReceiver publication;
    private QueueReceiver winner;
    private TopicPublisher update;
    private TopicPublisher top10;
    private  List<Player> players;

    public PublicZone() throws Exception {
        players = Collections.synchronizedList(new ArrayList<Player>());
        //At boot the admin object is created
        AdminModule.connect("root", "root", 60);
        javax.jms.ConnectionFactory cf = TcpConnectionFactory.create("localhost", 16010);
        javax.jms.QueueConnectionFactory qcf = QueueTcpConnectionFactory.create("localhost", 16010);
        javax.jms.TopicConnectionFactory tcf = TopicTcpConnectionFactory.create("localhost", 16010);
        javax.naming.Context jndiCtx = new javax.naming.InitialContext();
        jndiCtx.bind("cf", cf);
        jndiCtx.bind("qcf", qcf);
        jndiCtx.bind("tcf", tcf);
        jndiCtx.close();
        AdminModule.disconnect();
        //Adding the queue to receive the publication from user
        addQueue("Publication");
        Context ictx = new InitialContext();
        Queue queue = (Queue) ictx.lookup("Publication");
        QueueConnection qc = qcf.createQueueConnection();
        QueueSession qs = qc.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        publication = qs.createReceiver(queue);
        runPublicationThread();
        //Adding the queue to receive the winner from a match
        addQueue("Winner");
        Queue win = (Queue) ictx.lookup("Winner");
        winner = qs.createReceiver(win);
        //Registration of the topic update
        addTopic("Update");
        Topic topic = (Topic) ictx.lookup("Update");
        TopicConnection tc = tcf.createTopicConnection();
        TopicSession ts = tc.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
        update = ts.createPublisher(topic);
        runUpdateToAllThread();
        //Registration of the topic top10
        addTopic("top10");
        Topic top = (Topic) ictx.lookup("Top10");
        top10 = ts.createPublisher(top);
        ictx.close();
        runTop10Thread();
    }

    public void runTop10Thread() {
        Thread t = new Thread(new Runnable(){
            public void run() {
                //Body of the runTop10Thread
                Winner msg = null;
                while(true) {
                    try {
                        msg = (Winner)winner.receive(10000);
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
                        }
                    }
                    else {
                        String winner = msg.getWinner();
                        String other = msg.getOther();
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
                                players.add(toModify1);
                            }
                            else {
                                players.add(toModify2);
                            }
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
                //Body of the UpdateToAllThread
            }
        });
        t.start();
    }
    public void runPublicationThread() {
        Thread t = new Thread(new Runnable(){
            public void run() {
                //Body of the PublicationThread
            }
        });
        t.start();
    }
    // This method allows to create a queue inside the broker
    public void addQueue (String queueName) throws Exception{
        AdminModule.connect("root", "root", 60);
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
        AdminModule.connect("root", "root", 60);
        Topic topic = Topic.create(topicName);
        User.create("anonymous", "anonymous");
        topic.setFreeReading();
        topic.setFreeWriting();
        javax.naming.Context jndiCtx = new javax.naming.InitialContext();
        jndiCtx.bind(topicName, topic);
        jndiCtx.close();
        AdminModule.disconnect();
    }

}
