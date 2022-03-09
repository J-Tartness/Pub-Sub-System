package main;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Broker {
    String brokerName;
    int port;

    // Mutex for controlling priorityQueue poll
    private static Semaphore mutexAdd = new Semaphore(1);

    private static Semaphore mutexRemove = new Semaphore(1);

    //TimeDifference
    Map<String , Long> mapTimeDifference = new HashMap<>();

    Map<String, Set<String>> subscribersTopicMap = new HashMap<String, Set<String>>();
    Map<String, String> subscribers = new HashMap<String, String>();
    Map<String, Socket> onlineSubscribers = new HashMap<String, Socket>();

    Map<String, Integer> indexOfTopicPriorityQueue = new HashMap<String,Integer>();
    List<PriorityQueue<Message>> topicPriorityQueues = new ArrayList<PriorityQueue<Message>>();

    public Broker(String brokerName) throws IOException {
        this.brokerName = brokerName;
        this.port = 8088;

        File subscriberInfoFile = new File("subscriberInfo.txt");
        if (!subscriberInfoFile.exists()) {
            subscriberInfoFile.createNewFile();
        }

        File topicAndSubscriberFile = new File("topicAndSubscriber.txt");
        if (!topicAndSubscriberFile.exists()) {
            topicAndSubscriberFile.createNewFile();
        }

        init();
    }

    public void init() throws IOException {
        BufferedReader brOfTopicAndSubscriber = new BufferedReader(new FileReader("topicAndSubscriber.txt"));
        BufferedReader brOfSubscriberInfo = new BufferedReader(new FileReader("subscriberInfo.txt"));

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
            if(line.equals("")){
                continue;
            }
            String[] strs = line.split(" ");
            String subscriberName = strs[0];

            subscribers.put(subscriberName, strs[1]+":"+strs[2]);
        }

        brOfTopicAndSubscriber.close();
        brOfSubscriberInfo.close();

        int index = 0;
        for(String topic : subscribersTopicMap.keySet()){
            topicPriorityQueues.add(new PriorityQueue<Message>((v1,v2)-> (int) (v1.getTimestamp()-v2.getTimestamp())));
            indexOfTopicPriorityQueue.put(topic,index);
            index++;
        }
    }

    public void addSubscriber(String topic, String name) throws IOException {
        if(!subscribersTopicMap.containsKey(topic)){
            subscribersTopicMap.put(topic, new HashSet<>());
        }

        //modify relationship Table of Topic and Subscriber
        Set<String> set = subscribersTopicMap.get(topic);
        set.add(name);
        subscribersTopicMap.put(topic,set);

        BufferedReader brOfTopicAndSubscriber = new BufferedReader(new FileReader("topicAndSubscriber.txt"));
        ArrayList<String> list = new ArrayList<>();

        String line = null;
        while((line = brOfTopicAndSubscriber.readLine())!=null){
            String[] strs = line.split(" ");
            if(strs[0].equals(topic)){
                line = line+" "+name;
            }
            list.add(line);
        }

        BufferedWriter outputOftopicAndSub = new BufferedWriter(new FileWriter("topicAndSubscriber.txt"));
        for(String s:list){
            outputOftopicAndSub.write(s+"\n");
        }

        brOfTopicAndSubscriber.close();
        outputOftopicAndSub.close();
    }

    public void removeSubscriber(String topic, String name) throws IOException {
        Set<String> set = subscribersTopicMap.get(topic);
        set.remove(name);
        subscribersTopicMap.put(topic,set);

        BufferedReader brOfTopicAndSubscriber = new BufferedReader(new FileReader("topicAndSubscriber.txt"));
        ArrayList<String> list = new ArrayList<>();

        String line = null;
        while((line = brOfTopicAndSubscriber.readLine())!=null){
            String[] strs = line.split(" ");
            if(strs[0].equals(topic)){
                int index = line.indexOf(name);
                line = line.substring(0,index-1)+line.substring(index+name.length());
            }
            list.add(line);
        }

        BufferedWriter outputOftopicAndSub = new BufferedWriter(new FileWriter("topicAndSubscriber.txt"));
        for(String s:list){
            outputOftopicAndSub.write(s+"\n");
        }
        brOfTopicAndSubscriber.close();
        outputOftopicAndSub.close();
    }

    public void addMessageToQueue(String topic, Message message) throws InterruptedException {
        if(!indexOfTopicPriorityQueue.containsKey(message.getTopic())){
            return;
        }
        int index = indexOfTopicPriorityQueue.get(topic);
        if(mutexAdd.tryAcquire(2000, TimeUnit.MILLISECONDS)) {
            topicPriorityQueues.get(index).add(message);
            mutexAdd.release();
        }
    }

    public Message removeMessageToQueue(String topic, Message message) throws InterruptedException {
        if(!indexOfTopicPriorityQueue.containsKey(topic)){
            return null;
        }
        int index = indexOfTopicPriorityQueue.get(topic);
        if(mutexRemove.tryAcquire(2000, TimeUnit.MILLISECONDS)) {
            message = topicPriorityQueues.get(indexOfTopicPriorityQueue.get(topic)).poll();
            mutexRemove.release();
        }
        return message;
    }

    public void broadcast(String topic) throws IOException, InterruptedException {
        if(topicPriorityQueues.get(indexOfTopicPriorityQueue.get(topic)).isEmpty()){
            return;
        }

        Set<String> set = subscribersTopicMap.get(topic);
        Message message = null;
        message =  removeMessageToQueue(topic, message);
        System.out.println(message.toString());
        for(String subscriberName:set){
//            String[] strs = subscribers.get(subscriberName).split(" ");
//            String ip = strs[0];
//            int p = Integer.parseInt(strs[1]);
//            Socket s = new Socket(ip, p);
            Socket s = onlineSubscribers.getOrDefault(subscriberName, null);
            if(s==null){
                continue;
            }
            System.out.println("Send this message to:"+subscriberName);
            Thread sendMessageHandler = new Thread(new SendMessageHandler(s,message));
            sendMessageHandler.start();
        }
    }

    public static void main(String[] arg) throws IOException {
        Broker broker = new Broker("SBS");
        System.out.println("Broker "+broker.brokerName+" is online...\n");

        ServerSocket serverSocket = new ServerSocket(broker.port);
        Socket socket = null;

        Thread broadcastHandler = new Thread(new BroadcastHandler(broker));
        broadcastHandler.start();

        while(true) {
            socket = serverSocket.accept();
            Thread requestHandler=new Thread(new RequestHandler(socket,broker));
            requestHandler.start();
        }
    }
}

class BroadcastHandler extends Thread{
    Broker broker;

    public BroadcastHandler(Broker b){
        this.broker = b;
    }

    @Override
    public void run() {
        while(true){
            Scanner sc = new Scanner(System.in);
            String[] strs = sc.nextLine().split(" ");
            String command = strs[0];
            if(command.equals("broadcast")){
                String topic = strs[1];
                if(!broker.subscribersTopicMap.containsKey(topic)){
                    System.out.println("No such topic!");
                    continue;
                }
                try {
                    broker.broadcast(topic);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else if(command.equals("lsSub")){
                String topic = strs[1];
                Set<String> set= broker.subscribersTopicMap.get(topic);
                for(String name:set){
                    System.out.print(name+" ");
                }
                System.out.println();
            }
            else if(command.equals("lsOnline")){
                for(String name: broker.onlineSubscribers.keySet()){
                    System.out.print(name+" ");
                }
                System.out.println();
            }
            else{
                System.out.println("Error Command!");
            }
        }
    }
}


class RequestHandler extends Thread{
    Socket socket;
    Broker broker;
    boolean isRequiredToListen = false;
    String subName = null;
    boolean flag = true;

    public RequestHandler(Socket s, Broker b){
        this.socket = s;
        this.broker = b;
    }

    @Override
    public void run() {
        try {
            long lastHeartReceiveTime = System.currentTimeMillis();

            while(flag){
                if(isRequiredToListen && (System.currentTimeMillis()-lastHeartReceiveTime>3000)){
                    System.out.println("No Heart from "+subName+"! Socket Close!");
                    break;
                }

                if(socket.isClosed()){
                    break;
                }

                InputStream is = socket.getInputStream();
                if(is.available()>0){
                    ObjectInputStream ois=new ObjectInputStream(is);
                    Object obj = (Object)ois.readObject();
                    Long receiveTime = System.nanoTime();
                    if(obj instanceof  TimePub){
                        TimePub timePub = (TimePub) obj;
                        if(timePub.count == 1){
                            //System.out.println("收到第一条消息");
                            TimePub timePub1 = new TimePub(timePub.topic, 2, System.nanoTime() + receiveTime -timePub.timestamp);
                            //System.out.println(timePub1.toString());
                            new Thread(new SendMessageHandler(socket, timePub1)).start();
                        }else if(timePub.count == 3){
                            System.out.println("TimeDifference between " + timePub.getTopic() +" is:"+ timePub.getTimestamp()/2);
                            broker.mapTimeDifference.put(timePub.getTopic(), timePub.getTimestamp()/2);
                        }
                    }else if(obj instanceof  Message){
                        Message msg = (Message) obj;
                        if(msg.getTopic().equals("online")){
                            //modify subscribersInfo Table
                            String[] info = msg.getPayload().split(" ");
                            String name = info[0];
                            String ip = info[1];
                            String port = info[2];

                            if(broker.subscribers.containsKey(name)){
                                BufferedReader brOfSubscriberInfo = new BufferedReader(new FileReader("subscriberInfo.txt"));
                                ArrayList<String> list = new ArrayList<>();

                                String line = null;
                                while((line = brOfSubscriberInfo.readLine())!=null){
                                    String[] strs = line.split(" ");
                                    if(strs[0].equals(name)){
                                        line = name+" "+ip+" "+port+"\n";
                                    }
                                    list.add(line);
                                }

                                BufferedWriter outputOfSubscriberInfo = new BufferedWriter(new FileWriter("subscriberInfo.txt"));
                                for(String s:list){
                                    outputOfSubscriberInfo.write(s+"\n");
                                }

                                outputOfSubscriberInfo.close();
                                brOfSubscriberInfo.close();
                            }else{
                                BufferedWriter outputOfSubscriberInfo = new BufferedWriter(new FileWriter("subscriberInfo.txt",true));
                                outputOfSubscriberInfo.write(name+" "+ip+" "+port+"\n");
                                outputOfSubscriberInfo.close();
                            }
                            broker.subscribers.put(name, ip+":"+port);
                            broker.onlineSubscribers.put(name,socket);
                            System.out.println(name+" is"+" "+"online. Start Listening Heart!");
                            this.subName = name;
                            isRequiredToListen = true;
                        }
                        else if(msg.getTopic().equals("offline")){
                            String name = msg.getPayload();
                            broker.onlineSubscribers.remove(name);
                            socket.close();
                            System.out.println(name+" is"+" "+"offline");
                        }else{
                            String publisherName = msg.getTopic().substring(0,msg.getTopic().indexOf(":"));
                            String topic = msg.getTopic().substring(msg.getTopic().indexOf(":")+1);
                            msg.setTopic(topic);
                            msg.setTimestamp(msg.getTimestamp()-broker.mapTimeDifference.get(publisherName));
                            broker.addMessageToQueue(topic,msg);
                            System.out.println("Received message from "+publisherName+":"+ msg.getPayload());
                            //broker.broadcast(topic);
                        }
                    }else if(obj instanceof  TopicSub){
                        TopicSub topicSub = (TopicSub) obj;
                        if(topicSub.isSubscribe){
                            for(String topic : topicSub.topicSelected){
                                broker.addSubscriber(topic,topicSub.subscriberName);
                            }
                            System.out.println("Received subscription from " + topicSub.subscriberName + ": " + topicSub.topicSelected);
                        }
                        else{
                            for(String topic : topicSub.topicSelected){
                                broker.removeSubscriber(topic,topicSub.subscriberName);
                            }
                        }
                        if(!broker.onlineSubscribers.containsKey(topicSub.subscriberName)){
                            socket.close();
                        }
                    }
                    else if(obj instanceof  Heart){
                        lastHeartReceiveTime = System.currentTimeMillis();
                        new Thread( new SendMessageHandler(socket,new Heart("Alive"))).start();
                    }
                }
            }
            if(subName!=null){
                broker.onlineSubscribers.remove(subName);
            }
            if(!socket.isClosed()){
                socket.close();
            }
            System.out.println("Socket close!");
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
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
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }
}