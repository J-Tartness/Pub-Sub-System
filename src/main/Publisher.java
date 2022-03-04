package main;

import java.net.InetAddress;
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
        Socket socket = new Socket("127.0.0.1", publisher.port);
        OutputStream os = new DataOutputStream(socket.getOutputStream());
        ObjectOutputStream oos = new ObjectOutputStream(os);

        while(true){
            String[] msgContent = scanner.nextLine().split(" ");
            publisher.publish(new Message(msgContent[0],msgContent[1]) , oos);
        }
    }

    public void publish(Message message, ObjectOutputStream oos) throws IOException {
        oos.writeObject(message);
    }
}

