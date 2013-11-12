import java.util.Collections;
import java.io.Console;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Integer;
import java.lang.Character;
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
    private QueueSender winner;

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
            Queue winnerQueue = (Queue) ictx.lookup("Winner");

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
            winner = qs.createSender(winnerQueue);
            tsub.setMessageListener(new MsgListenerTop10(top10));
            tsub2.setMessageListener(new MsgListenerUpdate(announcements));
            tc.start();
            qc.start();
        }
        catch (javax.naming.NamingException e) {
            e.printStackTrace();
        }
        catch (JMSException e) {
            e.printStackTrace();
        }
    }



    public void game (QueueReceiver temprec, QueueSender tempsend, QueueSession qs, String username,String enemy) {
        TextMessage msg = null;
        char sign = ' ';
        char sign_enemy = ' ';
        boolean isMyTurn = false;
        char[][] board = new char[3][3];
        for (int i=0; i<3; i++)
            for (int j=0; j<3; j++)
                board[i][j] = ' ';
        //decisione segno
        Random generator =  new Random();
        if (generator.nextInt()%2 == 0) {
            sign = 'X';
            sign_enemy = 'O';
        }
        else {
            sign = 'O';
            sign_enemy = 'X';
        }
        try {
            TextMessage signToSend = qs.createTextMessage();
            signToSend.setText(java.lang.Character.toString(sign_enemy));
            System.out.println("Sending the sign to the player2 "+sign_enemy+", your is "+sign);
            tempsend.send(signToSend);
            String move;
            if (sign == 'X') isMyTurn = true;
            else isMyTurn = false;
            int result;
            do {
                if (isMyTurn) {
                    do {
                        printBoard(board);
                        move = System.console().readLine("Insert your move: ");
                    }while(!apply(board, move, sign));
                    TextMessage lst = qs.createTextMessage();
                    lst.setText(username+","+move);
                    tempsend.send(lst);
                }
                else {
                    boolean redo=false;;
                    do {
                        System.out.println("Receiving temprec");
                        msg = (TextMessage) temprec.receive();
                        System.out.println(msg.getText());
                        String[] tokens=msg.getText().split(",");
                        String name = tokens[0];
                        String info = tokens[1];
                        move = tokens[1];
                        if (name.compareTo(enemy) == 0) {
                            apply(board, move, sign_enemy);
                            redo = false;
                        }
                        else {
                            try {
                                Queue answer = (Queue) ictx.lookup(info);
                                QueueSender saybusy = qs.createSender(answer);
                                TextMessage ans = qs.createTextMessage();
                                ans.setText("BUSY");
                                saybusy.send(ans);
                                redo = true;
                            }
                            catch (javax.naming.NamingException e) {
                                e.printStackTrace();
                            }
                        }
                    }while(redo);
                }
                isMyTurn = !isMyTurn;
                String res = winner(board, sign,0);
                result = 0;
                if (res.compareTo("WIN") == 0) {
                    result = 2;
                    TextMessage win = qs.createTextMessage();
                    win.setText(username+","+enemy);
                    winner.send(win);
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    printBoard(board);
                    System.out.println("You are the winner congratulation!");
                }
                else if (res.compareTo("LOS") == 0) {
                    result = 1;
                    System.out.println("You are the loser!");
                }
                else if (res.compareTo("PAR") == 0){
                    result = 3;
                    System.out.println("The match is ended without a winner");
                }
                else{
                    result = 0;
                }
            }while (result == 0);

        }
        catch (JMSException e) {
            e.printStackTrace();
        }
    }


    boolean apply(char[][] board, String move, char sign) {
        try {
            int row = Integer.parseInt(Character.toString(move.charAt(0)));
            int col = Integer.parseInt(Character.toString(move.charAt(1)));
            if (board[row][col] == ' ') {
                board[row][col] = sign;
                return true;
            }
            else {
                return false;
            }
        }
        catch(NumberFormatException e) {
            return false;
        }
    }



    void printBoard(char[][] board) {
        System.out.println("   0 1 2 ");
        for (int i=0; i<3; i++) {
            for (int j=0; j<3; j++){
                if (j != 0)
                    System.out.print(board[i][j]+" ");
                else
                    System.out.print(i+") "+board[i][j]+" ");
            }
            System.out.println("");
        }
    }

    String winner(char[][] board, char sign, int round) {
        for (int i=0; i<3; i++) {
            if (board[i][0] == sign && board[i][1] == sign && board[i][2] == sign) return "WIN";
        }
        for (int i=0; i<3; i++) {
            if (board[0][i] == sign && board[1][i] == sign && board[2][i] == sign) return "WIN";
        }
        if (board[0][0] == sign && board[1][1] == sign && board[2][2] == sign) return "WIN";
        if (board[0][2] == sign && board[1][1] == sign && board[2][0] == sign) return "WIN";
        String res=new String("");
        if (round == 0) {
            if (sign == 'X')
                res = winner(board, 'O', 1);
            else
                res = winner(board, 'X', 1);
        }
        if (res.compareTo("WIN")==0) return "LOS";
        for (int i=0;i<3;i++) {
            for (int j=0;j<3;j++) {
                if (board[i][j] != ' ') return " ";
            }
        }
        return "PAR";
    }
    public void answer_match(String username) {
        synchronized(announcements) {
            boolean valid = true;
            char[][] board = new char[3][3];
            for (int i=0; i<3; i++)
                for (int j=0; j<3; j++)
                    board[i][j] = ' ';
            String move;
            Announcement a = null;
            do{
                try {
                     int choice = Integer.parseInt(System.console().readLine("Insert your choice: "));
                    a = (Announcement) announcements.get(choice);
                    valid = false;
                }
                catch (IndexOutOfBoundsException e ) {
                    System.out.println ("Value not valid try again");
                }
            }while (valid);
            Queue org = null;
            Queue mych = null;
            QueueSession qs= null;
            QueueSender organizer= null;
            QueueReceiver mychannel = null;
            char sign = ' ';
            try {
                org = (Queue) ictx.lookup(a.getNameChannel());
                qs = qc.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                organizer = qs.createSender(org);
                Random m = new Random();
                Integer n = new Integer(m.nextInt(10000));
                String mychan = new String(username+n.toString());
                System.out.println("nome canale "+mychan);
                try {
                    addQueue(mychan);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                mych = (Queue) ictx.lookup(mychan);
                TextMessage first = qs.createTextMessage();
                first.setText(username+","+mychan);
                System.out.println("Sending the first message "+username+","+mychan);
                organizer.send(first);
                System.out.println("Sended");
                mychannel = qs.createReceiver(mych);
                TextMessage msg = qs.createTextMessage();
                System.out.println("Receiving sign");
                qc.start();
                msg = (TextMessage) mychannel.receive();
                sign = msg.getText().charAt(0);
                boolean isMyTurn = false;
                char sign_enemy = 'X';
                if (sign == 'X') {
                    isMyTurn = true;
                    sign_enemy = 'O';
                }
                else {
                    isMyTurn = false;
                    sign_enemy = 'X';
                }
                int result;
                do {
                    if (isMyTurn) {
                        do {
                            printBoard(board);
                            move = System.console().readLine("Insert your move: ");
                        }while(!apply(board, move, sign));
                        TextMessage lst = qs.createTextMessage();
                        lst.setText(username+","+move);
                        organizer.send(lst);
                    }
                    else {
                        System.out.println("Receiving move");
                        msg = (TextMessage) mychannel.receive();
                        String[] tokens=msg.getText().split(",");
                        String name = tokens[0];
                        move  = tokens[1];
                        apply(board, move, sign_enemy);
                    }
                    isMyTurn = !isMyTurn;
                    String res = winner(board, sign,0);
                    result = 0;
                    if (res.compareTo("WIN") == 0) {
                        result = 2;
                        TextMessage win = qs.createTextMessage();
                        win.setText(username+","+a.getOwner());
                        winner.send(win);
                        System.out.print("\033[H\033[2J");
                        System.out.flush();
                        printBoard(board);
                        System.out.println("You are the winner congratulation!");
                    }
                    else if (res.compareTo("LOS") == 0) {
                        result = 1;
                        System.out.println("You are the loser!");
                    }
                    else if (res.compareTo("PAR") == 0){
                        result = 3;
                        System.out.println("The match is ended without a winner");
                    }
                    else{
                        result = 0;
                    }
                }while (result == 0);
            }
            catch (JMSException e) {
                e.printStackTrace();
            }
            catch (javax.naming.NamingException e) {
                e.printStackTrace();
            }


        }
    }
    public void publish_match(String username) {
        Random m = new Random();
        Integer n = new Integer(m.nextInt(10000));
        String toCreate = new String(username+n.toString());
        QueueSession qs = null;
        QueueSender tempsend = null;
        //Creation of the temporary queue to accept connection.
        try {
            addQueue(toCreate);
            Queue queue = (Queue) ictx.lookup(toCreate);
            qs = qc.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueReceiver temprec = qs.createReceiver(queue);
            //Creation and send to the public zone of the banner
            TextMessage msg = qs.createTextMessage();
            msg.setText(username+","+toCreate);
            System.out.println("Sending "+username+","+toCreate);
            publicator.send(msg);
            boolean first_message = false;
            do {
                msg = (TextMessage)temprec.receive(40000);
                if (msg != null) {
                    first_message = true;
                }
                else {
                    Console con =  System.console();
                    String choice = con.readLine("40 sec are passed, do you want republish the match [y/n] ?");
                    if (choice.compareTo("y") == 0) {
                        msg = qs.createTextMessage();
                        msg.setText(username+","+toCreate);
                        System.out.println("Sending "+username+","+toCreate);
                        publicator.send(msg);
                    }
                    else {
                        msg = qs.createTextMessage();
                        msg.setText(username+",-1");
                        System.out.println("Sending "+username+",-1");
                        publicator.send(msg);
                        return;
                    }
                }
            }while (!first_message);
            TextMessage msg2 = qs.createTextMessage();
            msg2.setText(username+",-1");
            System.out.println("Sending clear message");
            publicator.send(msg2);
            Queue queueS = null;
            String enemy = null;
            try {
                String[] tokens = msg.getText().split(",");
                enemy = tokens[0];
                String nameChannel = tokens[1];
                System.out.println("Nome Canale avversario "+nameChannel);
                queueS = (Queue) ictx.lookup(nameChannel);
                tempsend = qs.createSender(queueS);
            }
            catch (JMSException e) {
                e.printStackTrace();
            }
            System.out.println("Calling game");
            game(temprec, tempsend, qs, username, enemy);
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
            int i =0;
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
                System.out.println(i+") "+el.getOwner()+"|   "+el.getNameChannel());
                i++;
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
                answer_match(u, user_name);
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
    static void answer_match(User u, String username) {
        u.show_match();
        u.answer_match(username);
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

