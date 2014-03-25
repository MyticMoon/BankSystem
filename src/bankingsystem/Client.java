/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bankingsystem;

import java.net.*;
import java.io.*;
import java.util.Scanner;
/**
 *
 * @author Quan Hoang
 */
public class Client {
    public static void main(String[] args){
        DatagramSocket socket = null;
        try{
            /**
             * Initialize connection
             * Send hello message
             */
            socket = new DatagramSocket();        
            byte[] m = "hello".getBytes();
            InetAddress host;
            if (args.length < 1)
                host = InetAddress.getByName("localhost");
            else
                host = InetAddress.getByName(args[0]);
            int serverPort = 2222;
            
            //send request
            DatagramPacket request = new DatagramPacket(m,m.length,host,serverPort);
            socket.send(request);
            
            Scanner scanner = new Scanner(System.in);
            
            //recieve reply
            socket.setSoTimeout(1000);
            boolean monitoring = false;
            while(true){
                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer,buffer.length);
                try{ // reply message arrive
                    socket.receive(reply);
                    if((new String(reply.getData(),reply.getOffset(),reply.getLength())).equals("bye"))
                        return;
                    if((new String(reply.getData(),reply.getOffset(),reply.getLength())).equals("Start monitoring"))
                        monitoring = true;
                    else
                        monitoring = false;
                    System.out.println(new String(reply.getData()));
                    if(!monitoring){
                        String selection = scanner.nextLine();
                        request = new DatagramPacket(selection.getBytes(),selection.length(),host,serverPort);
                        socket.send(request);
                    }
                }catch(SocketTimeoutException e){
                    System.out.println(e);
                    System.out.println("Request resent!");
                    socket.send(request);
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