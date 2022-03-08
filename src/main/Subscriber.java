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
        System.out.println(ip);

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

                String[] brokerAddress = brokers.getOrDefault(strs[1], "").split(":");
                if(brokerAddress.length==0){
                    System.out.println("No such broker!");
                    continue;
                }

                Socket socket = null;
                if(onlineMap.containsKey(strs[1])){
                    socket = onlineMap.get(strs[1]);
                }
                else{
                    socket = new Socket(brokerAddress[0], Integer.parseInt(brokerAddress[1]));
                }

                if(command.equals("online")){
                    new Thread(new SendMessageHandler(socket, new Message("online",name+" "+ip+" "+port))).start();
                    new Thread(new MessageReceiver(socket)).start();
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

class MessageReceiver extends Thread{
    private Socket socket;

    public MessageReceiver(Socket s){
        this.socket = s;
    }

    @Override
    public void run() {
        try {
            while(true){
                if(socket.isClosed()){
                    break;
                }
                InputStream is = socket.getInputStream();
                ObjectInputStream ois=new ObjectInputStream(is);
                Object obj = (Object)ois.readObject();
                if(obj instanceof  Message){
                    Message msg = (Message) obj;
                    System.out.println("收到的消息：" + msg.toString());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
