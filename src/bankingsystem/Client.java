/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bankingsystem;


import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.*;
/**
 *
 * @author Quan Hoang
 */
public class Client {
    
    private static DatagramSocket socket;   
    
    private static byte[] marshall(String s){
        byte[] m = s.getBytes();          
        ByteBuffer buf = ByteBuffer.wrap(m);
        buf.order(ByteOrder.BIG_ENDIAN);
        byte[] n = new byte[buf.remaining()];
        buf.get(n);
        return n;
    }
    
    private static String unMarshall(byte[] m, int offset, int length){
        ByteBuffer buf = ByteBuffer.wrap(m);
        buf.order(ByteOrder.BIG_ENDIAN);
        byte[] n = new byte[buf.remaining()];
        buf.get(n);
        return new String(n,offset,length);
    }
    
    private static void send(InetAddress host, int port, DatagramPacket request){
        try{
            int ran=(int)(Math.random()*100);
            if (ran > 15){
                socket.send(request);
            }
        }catch(IOException e){
            System.out.println(e);
        }
    }
    
    public static void main(String[] args){
        socket = null;
        try{
            /**
             * Initialize connection
             * Send hello message
             */
            socket = new DatagramSocket();    
            InetAddress host;
                               
            if (args.length < 1)
                host = InetAddress.getByName("localhost");
            else
                host = InetAddress.getByName(args[0]);
            int serverPort = 2222;
            
            Date date = new Date();
            String init = "hello;" + date.getTime();
            
            /**
             * Marshalling
             */
            byte[] n = marshall(init);
            
            //send request
            DatagramPacket request = new DatagramPacket(n,n.length,host,serverPort);
            socket.send(request);
            
            Scanner scanner = new Scanner(System.in);
            
            //recieve reply     
            boolean monitoring = false;
            while(true){
                if(monitoring)
                    socket.setSoTimeout(0);
                else
                    socket.setSoTimeout(1000);
                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer,buffer.length);
                try{ // reply message arrive
                    socket.receive(reply);
                    Date d = new Date();
                    String rep = unMarshall(reply.getData(),reply.getOffset(),reply.getLength());
                    if(rep.equals("bye"))
                        return;
                    if(rep.equals("Start monitoring"))
                        monitoring = true;
                    if(rep.equals("Finish monitoring")){
                        monitoring = false;
                        System.out.println(new String(reply.getData()));
                        String ack = "ACK;" + d.getTime();
                        System.out.println("Sending 1: "+ack);
                        byte[] m = marshall(ack);
                        request = new DatagramPacket(m,m.length,host,serverPort);
                        send(host,serverPort,request);
                        continue;
                    }
                    System.out.println(new String(reply.getData()));
                    if(!monitoring){                       
                        String selection = scanner.nextLine() + ";" + d.getTime();
                        System.out.println("Sending 2: "+selection);
                        byte[] m = marshall(selection);
                        request = new DatagramPacket(m,m.length,host,serverPort);
                        send(host,serverPort,request);
                    }
                }catch(SocketTimeoutException e){
                    System.out.println(e);
                    System.out.println("Request resent!");
                    send(host,serverPort,request);
                }
            }
        }catch(ConnectException e){
            System.out.println("Error: " + e.toString());
        }catch(SocketException e){
            System.out.println("Error: " + e.toString());
        }catch(UnknownHostException e){
            System.out.println("Error: " + e.toString());
        }catch(IOException e){
            System.out.println("Error: " + e.toString());
        }finally{
            if (socket != null)
                socket.close();
        }
    }
}
