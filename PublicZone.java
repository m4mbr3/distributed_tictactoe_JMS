import java.io.*;
import javax.jms.*;
import javax.naming.*;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;
import org.objectweb.joram.client.jms.tcp.TopicTcpConnectionFactory;
import org.objectweb.joram.client.jms.tcp.QueueTcpConnectionFactory;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.Topic;
import javax.jms.JMSException;

class PublicZone {
    QueueReceiver publication;
    QueueReceiver winner;
    TopicPublisher update;
    TopicPublisher top10;
    public PublicZone() throws Exception {
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
                while(true) {
                    try {
                        winner.receive(10000);
                    }
                    catch (JMSException e) {
                        e.printStackTrace();
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
