package main;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.net.UnknownHostException;
import java.util.*;

public class Subscriber implements Serializable {
    private HashMap<String, String> subscriberTopicName;
    private String subscriberName;
    private int port;



    public Subscriber(String subscriberName, int port, HashMap<String, String> subscriberTopicName) {
        this.subscriberName = subscriberName;
        this.port = port;
        this.subscriberTopicName = subscriberTopicName;
    }

    public void addSubscribeTopic(String topic, Subscriber subscriber){

    }

    public void unSubscribeTopic(String topic, Subscriber subscriber){
    }

    public static void main(String[] args) throws UnknownHostException, IOException {
        System.out.println("please name the subscribe and give port!");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();
        int port = scanner.nextInt();
        List<String> topicSelected = new ArrayList<>();


        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println(ip);

        // access to serverinfo.txt to the server information.
        Socket socket = null;
        File serverinfo = new File("C://Users//18801//Desktop//serverinfo.txt");
        FileReader fileReader = new FileReader(serverinfo);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = null;
        String serverIP = "";
        int serverPort = 0;
        while ((line = bufferedReader.readLine()) != null) {
            boolean isconnected = false;
            String[] serverfields = line.split(" ");
            serverIP = serverfields[0];
            serverPort = Integer.valueOf(serverfields[1]);
            if(isconnected){
                break;
            }
        }
        try{
            socket = new Socket(serverIP, serverPort);
            System.out.println("输入" + name + "想要关注的topic: ");
            Scanner sc = new Scanner(System.in);
            String[] topicList = sc.nextLine().split(" ");
            for(String topic : topicList){
                topicSelected.add(topic);
            }
            new Thread(new SendTopic<>(socket, new TopicSub(name, topicSelected))).start();
            new Thread(new MessageReceiver(socket)).start();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

class SendTopic<T> extends Thread{
    Socket socket;
    TopicSub topicSub;

    public SendTopic(Socket s, TopicSub topicSub){
        this.socket = s;
        this.topicSub = topicSub;
    }

    @Override
    public void run() {
        try {
            OutputStream os = new DataOutputStream(socket.getOutputStream());
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(topicSub);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



class MessageReceiver extends Thread{
    private Socket socket;

    public MessageReceiver(Socket s){
        this.socket = s;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            ObjectInputStream ois=new ObjectInputStream(is);
            Object obj = (Object)ois.readObject();
            if(obj instanceof  Message){
                System.out.println("收到的消息：" + (Message) ois.readObject());
            }else if(obj instanceof  TopicSub){
                TopicSub topicSub = (TopicSub)obj;
                System.out.println(topicSub.subscriberName + "可以接受的topic是" + topicSub.topicSelected);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
