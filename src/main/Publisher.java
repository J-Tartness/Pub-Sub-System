package main;

import java.net.Socket;
import java.io.*;
import java.net.UnknownHostException;
import java.util.*;

public class Publisher {
    private String publisherName;
    private int port;

    public Publisher(String pubName) throws IOException {
        this.publisherName = pubName;
        this.port = 8088;
    }

    public static void main(String[] args) throws UnknownHostException, IOException{
//        if (args == null || args.length == 0) {
//            System.err.println("Usage: java PublisherHandler <publisher_name>");
//            System.exit(-1);
//        }

        Publisher publisher = new Publisher("Jake");

        Scanner scanner = new Scanner(System.in);

        Map<String, Socket> brokers = new HashMap<String,Socket>();
        Map<String, String> brokersAddress = new HashMap<String, String>();

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
            Socket socket = new Socket(serverfields[1], Integer.parseInt(serverfields[2]));

            TimePub timePub = new TimePub(publisher.publisherName, 1, System.nanoTime());
            System.out.println(timePub.toString());
            new Thread(new SendMessageHandler<>(socket, timePub)).start();
            new Thread(new receiveTime(socket)).start();

            brokers.put(serverfields[0],socket);
            brokersAddress.put(serverfields[0],serverfields[1]+":"+serverfields[2]);
        }

        while(Thread.activeCount()>2){
            Thread.yield();
        }

        try{
            while(true) {
                Scanner sc = new Scanner(System.in);
                String[] strs = sc.nextLine().split(" ");
                String brokerName = strs[0];

                Socket brokerSocket = brokers.getOrDefault(brokerName, null);
                if (brokerSocket==null) {
                    System.out.println("No such broker!");
                    continue;
                }

                if(!brokerSocket.isConnected()){
                    String[] address = brokersAddress.get(brokerName).split(":");
                    brokerSocket = new Socket(address[0], Integer.parseInt(address[1]));
                }

                new Thread(new SendMessageHandler(brokerSocket,new Message(publisher.publisherName+":"+strs[1],strs[2], System.nanoTime()))).start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}


class receiveTime implements Runnable {
    private Socket socket;

    public receiveTime(Socket s) {
        this.socket = s;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            Object obj = (Object) ois.readObject();
            TimePub timePub = (TimePub) obj;
            if (timePub.count == 2) {
                TimePub timePub1 = new TimePub(timePub.topic, 3, timePub.timestamp - System.nanoTime());
                System.out.println(timePub1.toString());
                new Thread(new SendMessageHandler(socket, timePub1)).start();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
