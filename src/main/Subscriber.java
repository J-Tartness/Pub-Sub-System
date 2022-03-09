package main;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.net.UnknownHostException;
import java.util.*;

public class Subscriber implements Serializable {
    private String subscriberName;
    private int port;

    public Subscriber(String subscriberName, int port) {
        this.subscriberName = subscriberName;
        this.port = port;
    }

    public static void main(String[] args) throws UnknownHostException, IOException {
        System.out.println("please name the subscribe and give port!");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();
        int port = scanner.nextInt();

        Map<String, String> brokers = new HashMap<String,String>();
        Map<String, Socket> onlineMap = new HashMap<>();

        String ip = InetAddress.getLocalHost().getHostAddress();
        //System.out.println(ip);

        // access to brokerInfo.txt to the server information.
        File brokerInfo = new File("brokerInfo.txt");
        if (!brokerInfo.exists()) {
            brokerInfo.createNewFile();
        }
        FileReader fileReader = new FileReader(brokerInfo);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            String[] serverfields = line.split(" ");
            brokers.put(serverfields[0],serverfields[1]+":"+serverfields[2]);
        }

        try{
            while(true){
                Scanner sc = new Scanner(System.in);
                String[] strs =  sc.nextLine().split(" ");
                String command = strs[0];

                if(command.equals("quit")){
                    for(Socket s:onlineMap.values()){
                        s.close();
                    }
                    break;
                }

                String[] brokerAddress = brokers.getOrDefault(strs[1], "").split(":");
                if(brokerAddress.length==0){
                    System.out.println("No such broker!");
                    continue;
                }

                Socket socket = null;
                if(onlineMap.containsKey(strs[1])){
                    socket = onlineMap.get(strs[1]);
                    if(socket.isClosed()){
                        socket = new Socket(brokerAddress[0], Integer.parseInt(brokerAddress[1]));
                    }
                }
                else{
                    socket = new Socket(brokerAddress[0], Integer.parseInt(brokerAddress[1]));
                }

                if(command.equals("online")){
                    new Thread(new SendMessageHandler(socket, new Message("online",name+" "+ip+" "+port))).start();
                    new Thread(new MessageReceiver(socket,strs[1])).start();
                    System.out.println("Start sending heart to "+strs[1]);
                    new Thread(new SendHeartHandler(socket)).start();
                    onlineMap.put(strs[1],socket);
                }
                else if(command.equals("offline")){
                    new Thread(new SendMessageHandler(socket, new Message("offline",name))).start();
                    onlineMap.remove(strs[1]);
                }
                else if(command.equals("subscribe")){
                    List<String> topicSelected = new ArrayList<>();
                    for(int i = 2;i<strs.length;i++){
                        topicSelected.add(strs[i]);
                    }
                    new Thread(new SendMessageHandler(socket, new TopicSub(true, name, topicSelected))).start();
                }
                else if(command.equals("unsubscribe")){
                    List<String> topicSelected = new ArrayList<>();
                    for(int i = 2;i<strs.length;i++){
                        topicSelected.add(strs[i]);
                    }
                    new Thread(new SendMessageHandler(socket, new TopicSub(false, name, topicSelected))).start();
                }
                else{
                    System.out.println("Error Command!");
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

class SendHeartHandler extends Thread{
    Socket socket;
    boolean flag = true;

    public SendHeartHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        long lastSendTime = System.currentTimeMillis();
        try{
            while(flag){
                if(socket.isClosed()){
                    break;
                }
                if(System.currentTimeMillis() - lastSendTime > 2000){
                    new Thread( new SendMessageHandler(socket,new Heart("Alive"))).start();
                    lastSendTime = System.currentTimeMillis();
                }
                else{
                    Thread.sleep(100);
                }
            }
        }catch (Exception e) {
            flag = false;
        }
    }
}

class MessageReceiver extends Thread{
    Socket socket;
    String name;
    boolean flag = true;

    public MessageReceiver(Socket s, String n){
        this.socket = s;
        this.name = n;
    }

    @Override
    public void run() {
        try {
            long lastHeartReceiveTime = System.currentTimeMillis();
            while(flag){
                if(socket.isClosed()){
                    break;
                }
                if(System.currentTimeMillis()-lastHeartReceiveTime>3000){
                    break;
                }

                InputStream is = socket.getInputStream();
                if(is.available()>0){
                    ObjectInputStream ois=new ObjectInputStream(is);
                    Object obj = (Object)ois.readObject();
                    if(obj instanceof  Message){
                        Message msg = (Message) obj;
                        System.out.println("Received "+msg.getTopic()+" message: "+msg.getPayload());
                    }
                    else if(obj instanceof  Heart){
                        lastHeartReceiveTime = System.currentTimeMillis();
                    }
                }
            }
            if(!socket.isClosed()){
                socket.close();
                System.out.println("No Heart from "+name+"! Socket Close!");
            }
        } catch (Exception e) {
            flag = false;
        }
    }
}
