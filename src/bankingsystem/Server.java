/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bankingsystem;


import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.sql.Timestamp;
/**
 *
 * @author Quan Hoang
 */
public class Server {
    // server's functions
    private static final String features_mes = "\nSelect one of the following options along with its parameters: \n"
                            + "1. Create new account\n" + "\t" + create_account()
                            + "\n2. Close an existing account\n" + "\t" + close_account()
                            + "\n3. Deposit\n" + "\t" + deposit()
                            + "\n4. Withdraw\n" + "\t" + withdraw()
                            + "\n5. Transfer money to another account\n" + "\t" + transfer()
                            + "\n6. Check recent transactions\n" + "\t" + transactions()
                            + "\n7. Monitor all account\n" + "\t" + monitor()
                            + "\n8. Close connection";
    private static HashMap<Identity,Long> clients; //all clients are stored here
    private static HashMap<Identity,String> last_mes;// clients' last reply 
    private static HashMap<Integer,BankAccount> bank;// all bank accounts
    private static DatagramSocket socket;
    private static HashMap<Identity,Date> monitorings;// all monitors
    private static HashMap<Identity,GeneralTimer> monitor_timers;// monitors' timer and ack timer
    private static boolean at_most_one; // true = server running in at most one mode
    
    //Timer class
    private static class GeneralTimer{
        private Timer timer; //timer for monitoring duration
        private Timer ackTimer; //timer for acknowledgement message
        
        public GeneralTimer(int duration, Identity monitor){
            final Identity tmpMonitor = monitor;
            timer = new Timer(duration*1000, new ActionListener() 
            { 
                @Override
                public void actionPerformed(ActionEvent e) 
                {   
                    System.out.println("action peformed");
                    if(monitorings.containsKey(tmpMonitor)){
                        monitorings.remove(tmpMonitor);
                        System.out.println("Deregister monitor");
                        System.out.println(monitorings.size());
                        send_rep(tmpMonitor,"Finish monitoring");
                        startAck();
                    }
                }
            });
            ackTimer = new Timer(1000,new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ea) {
                        send_rep(tmpMonitor,"Finish monitoring");
                }
            });
            timer.setRepeats(false);
            ackTimer.setRepeats(true);
        }
        
        public void startTimer(){
            timer.start();
            System.out.println(monitorings.size());
        }
        
        public void startAck(){
            System.out.println("resent finish monitoring");
            ackTimer.start();           
        }
        
        public void endAck(){
            ackTimer.stop();            
        }
    }
    
    //identity class for each client
    private static class Identity{
        private final InetAddress address;
        private final int port;
        
        public Identity(InetAddress address,int port){
            this.address = address;
            this.port = port;
        }
        
        public InetAddress get_address(){
            return address;
        }
        
        public int get_port(){
            return port;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                         + ((address == null) ? 0 : address.hashCode());
            result = prime * result + port;
            return result;
        }



        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (!(obj instanceof Identity))
                return false;

            Identity rhs = (Identity) obj;
            return rhs.get_address().equals(address) && rhs.get_port() == port;
        }
    }
    /**
     * marshalling and unmarshalling
     */
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
    
    //send message to client
    public static boolean send_rep(Identity iden,String message){
        int ran=(int)(Math.random()*100);
        if (ran > 15)
            try{
                byte[] m = marshall(message);
                socket.send(new DatagramPacket(m, m.length,iden.get_address(), iden.get_port()));
                return true;
            }
            catch(IOException ex){
                System.out.println(ex);
                return false;
            }
        else {
            System.out.println("Reply lost");
            return false;
        }
    }
    
    public static void main(String args[]){
        socket = null;
        bank = new HashMap<>();
        clients = new HashMap<>();
        monitorings = new HashMap<>();
        last_mes = new HashMap<>();
        monitor_timers = new HashMap<>();
        at_most_one = args.length < 1;
        
        try{
            System.out.println("Start server");
            socket = new DatagramSocket(2222);
            byte[] buffer = new byte[1000];
            
            while(true){
                //wait for request from client
                DatagramPacket request = new DatagramPacket(buffer,buffer.length);
                socket.receive(request);
                     
                String message = null;
                
                //get the message
                String input = unMarshall(request.getData(),request.getOffset(),request.getLength());
                String[] inputs = input.split("[;,]");
                boolean finish_trans = false;                             
                Long time = Long.parseLong(inputs[inputs.length - 1]);
                System.out.println("Received request from " + request.getAddress() + " on " + time + "mes: " + input);
                Identity iden = new Identity(request.getAddress(),request.getPort());
                
                try{
                    if(at_most_one)
                        if (time.equals(clients.get(iden)))
                            throw new Exception("Duplicate request");
                    //new client connected, register it
                    if (inputs[0].equals("hello")){                  
                        System.out.println("new client connected, ip:" + request.getAddress().toString());
                        message = features_mes;                   
                        if(!clients.containsKey(iden)){                      
                            clients.put(iden, Long.parseLong(inputs[inputs.length - 1]));
                        }
                        else{
                            clients.remove(iden);
                            clients.put(iden, Long.parseLong(inputs[inputs.length - 1]));
                            monitorings.remove(iden);
                            monitor_timers.remove(iden);
                            last_mes.remove(iden);
                        }
                    } 
                    //acknowledgement from client(when finish monitoring)
                    else if (inputs[0].equals("ACK")){
                        System.out.println("received ack");
                        //clear all the timer and server the client as normal
                        if (monitor_timers.containsKey(iden)){
                            System.out.println("remove timer");
                            GeneralTimer ti = monitor_timers.get(iden);
                            ti.endAck();
                            monitor_timers.remove(iden);
                            finish_trans = true;
                            message = "";
                        }
                        else{
                            message = "Wrong input";
                            finish_trans = true;
                        }
                    }
                    //perform other requests
                    else{
                        //handle requests                                         
                        try{
                            int client_choice = Integer.parseInt(inputs[0]);
                            //check duplicate

                            switch(client_choice){
                                case 1: //open account
                                    finish_trans = true;
                                    try{
                                        //check message validity
                                        if (inputs.length != 6)
                                            throw new Exception("Invalid parameters");
                                        String name = inputs[1];
                                        String password = inputs[2];
                                        if (password.length() != 8)
                                            throw new Exception("Password Length must be 8");

                                        boolean match = false;
                                        for(Currency e : Currency.values()){
                                            if (e.name().equals(inputs[3]))
                                                match = true;
                                        }
                                        if (!match)
                                            throw new Exception("Invalid currency type");
                                        Currency type = Currency.valueOf(inputs[3]);


                                        float balance = Float.parseFloat(inputs[4]);
                                        if (balance < 0)
                                            throw new Exception("Negative balance");
                                        
                                        message = create_account(name,password,type,balance);
                                    }catch(Exception e){
                                        message = "Error: Wrong request " + e.getMessage();
                                    }
                                    break;
                                case 2: //close account
                                    finish_trans = true;
                                    try{
                                        //check message validity
                                        if (inputs.length != 5)
                                            throw new Exception("Invalid parameters");
                                        String name = inputs[1];
                                        String password = inputs[2];                            
                                        int account_number = Integer.parseInt(inputs[3]);
                                        message = close_account(name,password,account_number);
                                    }catch(Exception e){
                                        message = "Error: Wrong request " + e.getMessage();
                                    }
                                    break;
                                case 3://deposit
                                    finish_trans = true;
                                    try{
                                        //check message validity
                                        if (inputs.length != 7)
                                            throw new Exception("Invalid parameters");
                                        int acc_no = Integer.parseInt(inputs[1]);
                                        String name = inputs[2];
                                        String password = inputs[3];                               
                                        boolean match = false;
                                        for(Currency e : Currency.values()){
                                            if (e.name().equals(inputs[4]))
                                                match = true;
                                        }
                                        if (!match)
                                            throw new Exception("Invalid currency type");
                                        Currency type = Currency.valueOf(inputs[4]);                                                            
                                        float amount = Float.parseFloat(inputs[5]); 

                                        message = deposit(acc_no,name,password,type,amount);
                                    }catch(Exception e){
                                        message = "Error: Wrong request " + e.getMessage();
                                    }
                                    break;
                                case 4: //withdraw
                                    finish_trans = true;
                                    try{
                                        //check message validity
                                        if (inputs.length != 7)
                                            throw new Exception("Invalid parameters");
                                        int acc_no = Integer.parseInt(inputs[1]);
                                        String name = inputs[2];
                                        String password = inputs[3];                               
                                        boolean match = false;
                                        for(Currency e : Currency.values()){
                                            if (e.name().equals(inputs[4]))
                                                match = true;
                                        }
                                        if (!match)
                                            throw new Exception("Invalid currency type");
                                        Currency type = Currency.valueOf(inputs[4]);                                                            
                                        float amount = Float.parseFloat(inputs[5]); 

                                        message = withdraw(acc_no,name,password,type,amount);
                                    }catch(Exception e){
                                        message = "Error: Wrong request " + e.getMessage();
                                    }
                                    break;
                                case 5://transfer money
                                    finish_trans = true;
                                    try{
                                        //check message validity
                                        if (inputs.length != 9)
                                            throw new Exception("Invalid parameters");
                                        int acc_no1 = Integer.parseInt(inputs[1]);
                                        String name1 = inputs[2];
                                        String password1 = inputs[3];                               
                                        boolean match = false;
                                        for(Currency e : Currency.values()){
                                            if (e.name().equals(inputs[4]))
                                                match = true;
                                        }
                                        if (!match)
                                            throw new Exception("Invalid currency type");
                                        Currency type = Currency.valueOf(inputs[4]); 
                                        int acc_no2 = Integer.parseInt(inputs[5]);
                                        String name2 = inputs[6];
                                        float amount = Float.parseFloat(inputs[7]); 

                                        message = transfer(acc_no1,name1,password1,type,acc_no2,name2,amount);
                                    }catch(Exception e){
                                        message = "Error: Wrong request " + e.getMessage();
                                    }
                                    break;
                                case 6://print all transactions
                                    finish_trans = true;
                                    try{
                                        //check message validity
                                        if (inputs.length != 5)
                                            throw new Exception("Invalid parameters");
                                        int acc_no = Integer.parseInt(inputs[1]);
                                        String name = inputs[2];
                                        String password = inputs[3];                               

                                        message = transactions(acc_no,name,password);
                                    }catch(Exception e){
                                        message = "Error: Wrong request " + e.getMessage();
                                    }
                                    break;
                                case 7://register to be monitor
                                    try{
                                        //check message validity
                                        if (inputs.length != 3)
                                            throw new Exception("Invalid parameters");
                                        int duration = Integer.parseInt(inputs[1]);                                                             
                                        message = register_monitor(request.getAddress(),request.getPort(),duration);
                                    }catch(Exception e){
                                        finish_trans = true;
                                        message = "Error: Wrong request " + e.getMessage();
                                    }
                                    break;
                                case 8://close connection
                                    message = "bye";
                                    clients.remove(iden);
                                    last_mes.remove(iden);
                                    System.out.println("Client disconnected ip:" + iden.get_address().toString());
                                    break;
                            }
                        }catch(NumberFormatException e){
                            message = "Error: Wrong function selection";
                        }
                    }
                    //return reply to user
                    if(finish_trans){
                        message = message + features_mes;
                    }
                    //update records
                    last_mes.remove(iden);
                    last_mes.put(iden, message);

                    clients.remove(iden);
                    clients.put(iden,time);

                    //send the message back to client
                    DatagramPacket reply = new DatagramPacket(message.getBytes(),message.length(),request.getAddress(),request.getPort());
                    
                    send_rep(iden,message);
                }catch(Exception e){
                    //if duplicate message found
                    System.out.println(e);
                    message = last_mes.get(iden);
                    finish_trans = false;   
                    last_mes.remove(iden);
                    last_mes.put(iden, message);

                    clients.remove(iden);
                    clients.put(iden,time);

                    DatagramPacket reply = new DatagramPacket(message.getBytes(),message.length(),request.getAddress(),request.getPort());

                    send_rep(iden,message);
                }
            }
        }catch(SocketException e){
            System.out.println("Error: " + e.toString());
        }catch(IOException e){
            System.out.println("Error: " + e.toString());
        }
        finally{
            if (socket != null)
                socket.close();
        }
    }
    
    /**
     * create new account
     * @return 
     */
    private static String create_account(){
        String currencies = "";
        for(Currency e: Currency.values())
            currencies = currencies + e.name() + ", ";
        return "Specify your name, password (length of 8), currency type (" 
                + currencies + ") "
                + "and balance, use '; or ,' to seperate inputs";
    }
    
    private static String create_account(String name, String password, Currency type, float balance){
        BankAccount account = new BankAccount(name,password,type,balance);
        int acc_no = account.get_account_number();
        bank.put(acc_no, account);
        String result = Integer.toString(acc_no);
        String message = "\ncreate new account:"
                + "\nnumber: " + acc_no
                + "\nname: " + name
                + "\ntype: " + type.name()
                + "\nbalance: " + balance;
        inform_monitors(message);
        System.out.println(message + "\npassword: " + password);
        return result;
    }
    
    /**
     * Close account
     * @return 
     */
    private static String close_account(){
        return "Specify your name, password "
                + "and account number, use '; or ,' to seperate inputs";
    }
    private static String close_account(String name,String password,int account_number){
        if (!bank.containsKey(account_number))
            return "Account does not exist";
        BankAccount account = bank.get(account_number);
        if (!account.get_password().equals(password))
            return "Password incorrect";
        if (!account.get_name().equals(name))
            return "Account name is not matched";
        String output="\nclose account:"
                + "\nnumber: " + account_number
                + "\nname: " + name;
        inform_monitors(output);        
        System.out.println(output+"\npassword: " + password);
                
        return "Sucessfully close your account";
    }
    
    /**
     * Deposit
     * @return 
     */
    private static String deposit(){
        String currencies = "";
        for(Currency e: Currency.values())
            currencies = currencies + e.name() + ", ";
        return "Specify your account number, name, password, currency type (" 
                + currencies + ") "
                + "and amount, use '; or ,' to seperate inputs";
    }
    
    private static String deposit(int account_number, String name, String password, Currency type, float amount){
        if (!bank.containsKey(account_number))
            return "Account does not exist";
        BankAccount account = bank.get(account_number);
        if (!account.get_password().equals(password))
            return "Password incorrect";
        if (!account.get_name().equals(name))
            return "Account name is not matched";
        if (type != account.get_type())
            return "Cross currency not supported, choose different currency";
        if (amount < 0)
            return "Amount should be > 0. Choose widthdraw";
        account.set_balance(account.get_balance() + amount);
        String output ="\nupdate balance"
                + "\nnumber: " + account_number
                + "\nname: " + name
                + "\nold balance: " + (account.get_balance() - amount)
                + "\nnew balance: " + account.get_balance();
        System.out.println(output + "\npassword: " + password);
        Date date = new Date();
        account.add_event(amount, new Timestamp(date.getTime()));
        inform_monitors(output);
        return Float.toString(account.get_balance());
    }
    
    /**
     * Withdraw
     * @return 
     */
    private static String withdraw(){
        String currencies = "";
        for(Currency e: Currency.values())
            currencies = currencies + e.name() + ", ";
        return "Specify your account number, name, password, currency type (" 
                + currencies + ") "
                + "and amount, use '; or ,' to seperate inputs";
    }
    
    private static String withdraw(int account_number, String name, String password, Currency type, float amount){
        if (!bank.containsKey(account_number))
            return "Account does not exist";
        BankAccount account = bank.get(account_number);
        if (!account.get_password().equals(password))
            return "Password incorrect";
        if (!account.get_name().equals(name))
            return "Account name is not matched";
        if (type != account.get_type())
            return "Cross currency not supported, choose different currency";
        if (amount > account.get_balance())
            return "Not enough balance";
        account.set_balance(account.get_balance() - amount);
        String output="\nupdate balance"
                + "\nnumber: " + account_number
                + "\nname: " + name
                + "\nold balance: " + (account.get_balance() + amount)
                + "\nnew balance: " + account.get_balance();
        inform_monitors(output);
        System.out.println(output+ "\npassword: " + password);
        Date date = new Date();
        account.add_event(-amount, new Timestamp(date.getTime()));
        return Float.toString(account.get_balance());
    }
    
    /**
     * Transfer
     * @return 
     */
    private static String transfer(){
        String currencies = "";
        for(Currency e: Currency.values())
            currencies = currencies + e.name() + ", ";
        return "Specify your account number, name, password, currency type (" 
                + currencies + ") "
                + "Target account number, name"
                + " and amount, use '; or ,' to seperate inputs";
    }
    
    private static String transfer(int account_number1, String name1, String password, Currency type, int account_number2, String name2, float amount){
        if (!bank.containsKey(account_number1))
            return "Account does not exist";
        BankAccount account1 = bank.get(account_number1);
        if (!account1.get_password().equals(password))
            return "Password incorrect";
        if (!account1.get_name().equals(name1))
            return "Account name is not matched";
        if (!bank.containsKey(account_number2))
            return "Target account does not exist";
        BankAccount account2 = bank.get(account_number2);
        if (!account2.get_name().equals(name2))
            return "Targe account name is not matched";
        if (account2.get_type() != account1.get_type())
            return "Cross currency not supported";
        if (amount < 0)
            return "Transfered amount must be > 0";
        if (amount > account1.get_balance())
            return "Not enough balance";
        account1.set_balance(account1.get_balance() - amount);
        account2.set_balance(account2.get_balance() + amount);
        String output="\ntransfer"
                + "\nnumber: " + account_number1
                + "\nname: " + name1
                + "\npassword: " + password
                + "\nold balance: " + (account1.get_balance() + amount)
                + "\nnew balance: " + account1.get_balance()
                + "\n to number: " + account_number2
                + "\n name: " + name2
                + "\nold balance: " + (account2.get_balance() - amount)
                + "\nnew balance: " + account2.get_balance();
        inform_monitors(output);
        System.out.println(output+ "\npassword: " + password);
                
        Date date = new Date();
        account1.add_event(-amount, new Timestamp(date.getTime()));
        account2.add_event(amount, new Timestamp(date.getTime()));
        return Float.toString(account1.get_balance());
    }
    
    /**
     * Transactions
     */
    
    private static String transactions(){
        return "Specify your account number, name, and password use '; or ,' to seperate inputs";
    }
    
    private static String transactions(int account_number, String name, String password){
        if (!bank.containsKey(account_number))
            return "Account does not exist";
        BankAccount account = bank.get(account_number);
        if (!account.get_password().equals(password))
            return "Password incorrect";
        if (!account.get_name().equals(name))
            return "Account name is not matched";
        String output = "\ncheck transaction"
                + "\nnumber: " + account_number
                + "\nname: " + name;
        inform_monitors(output);
        System.out.println(output+ "\npassword: " + password);
        return account.get_events();
    }
    
    /**
     * monitor
     */
    private static String monitor(){
        return "Specify monitoring duration in seconds";
    }
    
    private static String register_monitor(InetAddress address, int port, int duration){
        Identity monitor = new Identity(address,port);
        if(!monitorings.containsKey(monitor)){
            Date date = new Date();
            int end = (int) (date.getTime() + duration * 1000);
            Date end_date = new Date(end);
            monitorings.put(monitor, end_date);
            GeneralTimer gTimer = new GeneralTimer(duration, monitor);
            gTimer.startTimer();
            monitor_timers.put(monitor, gTimer);
        }
        //tiempo.setRepeats(false);
        //tiempo.start();
        
        return "Start monitoring";
    }
    
    
    
    private static void inform_monitors(String message){
        Iterator<Identity> iterator = monitorings.keySet().iterator();
        while(iterator.hasNext()){
            Identity monitor = iterator.next();
            String rep_mes;
            rep_mes = message;
            send_rep(monitor,rep_mes);
        }
    }
}
