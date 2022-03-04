package main;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.*;

public class Subscriber {
    private String subscriberName;
    private int port;

    public Subscriber(String subscriberName) {
        this.subscriberName = subscriberName;
        this.port = 8088;
    }

    public void subscribeTopic(String topic){

    }

    public void unsubscribeTopic(String topic){

    }

    public static void main(String[] args) throws IOException{
        Subscriber subscriber = new Subscriber("123");
        Scanner scanner = new Scanner(System.in);

        Socket postSocket = new Socket("127.0.0.1", subscriber.port);
        OutputStream os = new DataOutputStream(postSocket.getOutputStream());
        ObjectOutputStream oos = new ObjectOutputStream(os);

        ServerSocket serverSocket = new ServerSocket(subscriber.port);
        Socket receiveSocket = null;

        while(true) {
            receiveSocket = serverSocket.accept();
            Thread messageReceiver=new Thread(new messageReceiver(receiveSocket));
            messageReceiver.start();
        }
    }

}

class messageReceiver extends Thread{
    private Socket socket;

    public messageReceiver(Socket s){
        this.socket = s;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            ObjectInputStream ois=new ObjectInputStream(is);
            System.out.println("收到的消息：" + (Message) ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
