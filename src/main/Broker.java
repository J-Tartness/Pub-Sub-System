package main;

import java.io.*;
import java.net.*;

public class Broker {
    private String brokerName;
    private int port;

    public void addSubscriber(String topic, String subscriber){

    }

    public void addMessageToQueue(String topic, Message message){

    }

    public void removeSubscriber(String topic, String subscriber){

    }

    public void broadcast(String topic){

    }

    public static void main(String[] arg) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8088);
        Socket socket = null;

        while(true) {
            socket = serverSocket.accept();
            Thread RequestHandler=new Thread(new RequestHandler(socket));
            RequestHandler.start();
        }
    }
}

class RequestHandler extends Thread{
    private Socket socket;

    public RequestHandler(Socket s){
        this.socket = s;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            ObjectInputStream ois=new ObjectInputStream(is);
            System.out.println("客户端发送的对象：" + (Message) ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

