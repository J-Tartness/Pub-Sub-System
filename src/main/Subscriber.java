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
            new Thread(new WriteHandlerThread(socket)).start();
            new Thread(new ReadHandlerThread(socket)).start();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
class ReadHandlerThread implements  Runnable{
    private Socket socket;
    public  ReadHandlerThread(Socket socket){
        this.socket = socket;
    }
    @Override
    public void run() {
        DataInputStream dis = null;
        try{
            while(true){
                dis = new DataInputStream(socket.getInputStream());
                String receive = dis.readUTF();
                System.out.println("broker发过来的是： " + receive);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try{
                if(dis != null){
                    dis.close();
                }
                if(socket != null){
                    socket = null;
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }

    }
}
class WriteHandlerThread implements  Runnable{
    private Socket client;
    public  WriteHandlerThread(Socket client){
        this.client = client;
    }

    @Override
    public void run() {
        DataOutputStream dos = null;
        BufferedReader br = null;
        try{
            while(true){
                dos = new DataOutputStream(client.getOutputStream());
                System.out.println("请输入：\t");
                br = new BufferedReader(new InputStreamReader(System.in));
                String send = br.readLine();
                dos.writeUTF(send);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            try{
                if(dos != null){
                    dos.close();
                }
                if(br != null){
                    br.close();
                }
                if(client != null){
                    client = null;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
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
