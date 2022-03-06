package main;

import java.io.*;
import java.net.*;
import java.util.*;

public class Broker {
    String brokerName;
    int port;

    //TimeDifference
    Map<String , Long> mapTimeDifference = new HashMap<>();

    Map<String, Set<String>> subscribersTopicMap = new HashMap<String, Set<String>>();
    Map<String, String> subscribers = new HashMap<String, String>();

    Map<String, Integer> indexOfTopicPriorityQueue = new HashMap<String,Integer>();
    List<PriorityQueue<Message>> topicPriorityQueues = new ArrayList<PriorityQueue<Message>>();

    File topicAndSubscriberFile;
    File subscriberInfoFile;

    public Broker(String brokerName) throws IOException {
        this.brokerName = brokerName;
        this.port = 8088;

        subscriberInfoFile = new File("subscriberInfo.txt");
        if (!subscriberInfoFile.exists()) {
            subscriberInfoFile.createNewFile();
        }

        topicAndSubscriberFile = new File("topicAndSubscriber.txt");
        if (!topicAndSubscriberFile.exists()) {
            topicAndSubscriberFile.createNewFile();
        }

        init();
    }

    public void init() throws IOException {
        BufferedReader brOfTopicAndSubscriber = new BufferedReader(new FileReader(topicAndSubscriberFile));
        BufferedReader brOfSubscriberInfo = new BufferedReader(new FileReader(subscriberInfoFile));

        String line = null;
        while((line = brOfTopicAndSubscriber.readLine())!=null){
            String[] strs = line.split(" ");
            String topic = strs[0];

            Set<String> set = new HashSet<String>();
            for(int i = 1;i<strs.length;i++){
                set.add(strs[i]);
            }
            subscribersTopicMap.put(topic,set);
        }

        while((line = brOfSubscriberInfo.readLine())!=null){
            String[] strs = line.split(" ");
            String subscriberName = strs[0];

            subscribers.put(subscriberName, strs[1]+":"+strs[2]);
        }

        brOfTopicAndSubscriber.close();
        brOfSubscriberInfo.close();

        int index = 0;
        for(String topic : subscribersTopicMap.keySet()){
            topicPriorityQueues.add(new PriorityQueue<Message>());
            indexOfTopicPriorityQueue.put(topic,index);
            index++;
        }
    }

    public void addSubscriber(String topic, String[] subscriberInfo) throws IOException {
        String subscriberName = subscriberInfo[0];
        String ip = subscriberInfo[1];
        String port = subscriberInfo[2];

        //modify relationship Table of Topic and Subscriber
        Set<String> set = subscribersTopicMap.get(topic);
        set.add(subscriberName);
        subscribersTopicMap.put(topic,set);

        BufferedReader brOfTopicAndSubscriber = new BufferedReader(new FileReader(subscriberInfoFile));
        BufferedWriter outputOftopicAndSub = new BufferedWriter(new FileWriter(topicAndSubscriberFile));

        String line = null;
        while((line = brOfTopicAndSubscriber.readLine())!=null){
            String[] strs = line.split(" ");
            if(strs[0].equals(topic)){
                line = line+" "+subscriberName;
            }
            outputOftopicAndSub.write(line);
        }
        brOfTopicAndSubscriber.close();
        outputOftopicAndSub.close();

        //modify subscribersInfo Table
        if(subscribers.containsKey(subscriberName)){
            BufferedWriter outputOfSubscriberInfo = new BufferedWriter(new FileWriter(subscriberInfoFile));
            BufferedReader brOfSubscriberInfo = new BufferedReader(new FileReader(subscriberInfoFile));

            while((line = brOfSubscriberInfo.readLine())!=null){
                String[] strs = line.split(" ");
                if(strs[0].equals(subscriberName)){
                    line = subscriberName+" "+ip+" "+port+"\n";
                }
                outputOfSubscriberInfo.write(line);
            }
            outputOfSubscriberInfo.close();
            brOfSubscriberInfo.close();
        }
        else{
            BufferedWriter outputOfSubscriberInfo = new BufferedWriter(new FileWriter(subscriberInfoFile,true));
            outputOfSubscriberInfo.write(subscriberName+" "+ip+" "+port+"\n");
            outputOfSubscriberInfo.close();
        }
        subscribers.put(subscriberName, ip+":"+port);
    }

    public void removeSubscriber(String topic, String[] subscriberInfo) throws IOException {
        String subscriberName = subscriberInfo[0];

        Set<String> set = subscribersTopicMap.get(topic);
        set.remove(subscriberName);
        subscribersTopicMap.put(topic,set);

        BufferedReader brOfTopicAndSubscriber = new BufferedReader(new FileReader(subscriberInfoFile));
        BufferedWriter outputOftopicAndSub = new BufferedWriter(new FileWriter(topicAndSubscriberFile));

        String line = null;
        while((line = brOfTopicAndSubscriber.readLine())!=null){
            String[] strs = line.split(" ");
            if(strs[0].equals(topic)){
                int index = line.indexOf(subscriberName);
                line = line.substring(0,index-1)+line.substring(index+subscriberName.length());
            }
            outputOftopicAndSub.write(line);
        }
        brOfTopicAndSubscriber.close();
        outputOftopicAndSub.close();
    }

    public void addMessageToQueue(String topic, Message message){
        if(!indexOfTopicPriorityQueue.containsKey(message.getTopic())){
            return;
        }
        int index = indexOfTopicPriorityQueue.get(topic);
        topicPriorityQueues.get(index).add(message);
    }

    public void broadcast(String topic) throws IOException {
        if(topicPriorityQueues.get(indexOfTopicPriorityQueue.get(topic)).isEmpty()){
            return;
        }

        Set<String> set = subscribersTopicMap.get(topic);
        Message message = topicPriorityQueues.get(indexOfTopicPriorityQueue.get(topic)).poll();

        for(String subscriberName:set){
            String[] strs = subscribers.get(subscriberName).split(" ");
            String ip = strs[0];
            int p = Integer.parseInt(strs[1]);
            Socket s = new Socket(ip, p);

            Thread sendMessageHandler = new Thread(new SendMessageHandler(s,message));
            sendMessageHandler.start();
        }
    }

    public static void main(String[] arg) throws IOException {
        Broker broker = new Broker("broker");

        ServerSocket serverSocket = new ServerSocket(broker.port);
        Socket socket = null;

        while(true) {
            socket = serverSocket.accept();
            Thread requestHandler=new Thread(new RequestHandler(socket,broker));
            requestHandler.start();

            while(Thread.activeCount()>2){
                Thread.yield();
            }

            System.out.println(broker.topicPriorityQueues.get(0).size());
        }
    }
}

class RequestHandler extends Thread{
    Socket socket;
    Broker broker;

    public RequestHandler(Socket s, Broker b){
        this.socket = s;
        this.broker = b;
    }

    @Override
    public void run() {
        try {
            while(true){
                Long receiveTime = System.nanoTime();
                InputStream is = socket.getInputStream();
                ObjectInputStream ois=new ObjectInputStream(is);
                Object obj = (Object)ois.readObject();
                if(obj instanceof  TimePub){
                    TimePub timePub = (TimePub) obj;
                    if(timePub.count == 1){
                        System.out.println("收到第一条消息");
                        TimePub timePub1 = new TimePub(timePub.topic, 2, System.nanoTime() + receiveTime -timePub.timestamp);
                        System.out.println(timePub1.toString());
                        new Thread(new SendMessageHandler(socket, timePub1)).start();
                    }else if(timePub.count == 3){
                        System.out.println("..........");
                        System.out.println("TimeDifference: " + timePub.getTopic() +" "+ timePub.getTimestamp()/2);
                        broker.mapTimeDifference.put(timePub.getTopic(), timePub.getTimestamp()/2);
                    }

                }else if(obj instanceof  Message){
                    System.out.println("............");
                    Message msg = (Message) obj;
                    broker.addMessageToQueue(msg.getTopic(),msg);
                    System.out.println("receive message from publisher：" + msg.toString());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

class SendMessageHandler<T> extends Thread{
    Socket socket;
    T msg;

    public SendMessageHandler(Socket s, T m){
        this.socket = s;
        this.msg = m;
    }

    @Override
    public void run() {
        try {
            OutputStream os = new DataOutputStream(socket.getOutputStream());
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

