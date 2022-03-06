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

    public static void main(String[] args) throws IOException{
//        if (args == null || args.length == 0) {
//            System.err.println("Usage: java PublisherHandler <publisher_name>");
//            System.exit(-1);
//        }


        Publisher publisher = new Publisher("11");

        Scanner scanner = new Scanner(System.in);

        File serverinfo = new File("C://Users//18801//Desktop//serverinfo.txt");
        FileReader fileReader = new FileReader(serverinfo);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = null;
        String serverIP = "";
        int serverPort = 0;
        Socket socket = null;

        String t1 = "";
        String t4 = "";
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
            String[] timeContent = scanner.nextLine().split(" ");
            TimePub timePub = new TimePub(timeContent[0], Integer.parseInt(timeContent[1]), System.nanoTime());
            System.out.println(timePub.toString());
            new Thread(new SendMessageHandler<>(socket, timePub)).start();
            new Thread(new receiveTime(socket)).start();
            while(Thread.activeCount()>2){
                Thread.yield();
            }
            while(true){
                System.out.println("请输入message:  ");
                String[] msgContent = scanner.nextLine().split(" ");
                new Thread(new SendMessageHandler(socket,new Message(msgContent[0],msgContent[1],
                        System.nanoTime()))).start();
            }



        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void publish(Message message, ObjectOutputStream oos) throws IOException {
        oos.writeObject(message);
    }
}



class receiveTime implements Runnable {
    private Socket socket;

    public receiveTime(Socket s) {
        this.socket = s;
    }

    boolean first = true;

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
                new Thread(new SendMessageHandler<>(socket, timePub1)).start();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
