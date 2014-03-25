/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bankingsystem;

import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.Timestamp;
/**
 *
 * @author Quan Hoang
 */
public class Server {
    private static String features_mes = "\nSelect one of the following options: \n"
                            + "1. Create new account\n"
                            + "2. Close an existing account\n"
                            + "3. Deposit\n"
                            + "4. Widthraw\n"
                            + "5. Transfer money to another account\n"
                            + "6. Check recent transactions\n"
                            + "7. Monitor all account\n"
                            + "8. Close connection";
    private static HashMap<InetAddress,Integer> clients;
    private static HashMap<Integer,BankAccount> bank;
    private static DatagramSocket socket;
    private static HashMap<Identity,Date> monitorings;
    
    private static class Identity{
        private InetAddress address;
        private int port;
        
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
    }
    
    public static void main(String args[]){
        socket = null;
        bank = new HashMap<>();
        clients = new HashMap<>();
        monitorings = new HashMap<>();
        
        try{
            System.out.println("Start server");
            socket = new DatagramSocket(2222);
            byte[] buffer = new byte[1000];
            
            while(true){
                DatagramPacket request = new DatagramPacket(buffer,buffer.length);
                socket.receive(request);
                     
                String message = null;
                String input = new String(request.getData(),request.getOffset(),request.getLength());
                String[] inputs = input.split("[;,]");
                boolean finish_trans = false;
                if (input.equals("hello")){
                    System.out.println("new client connected, ip:" + request.getAddress().toString());
                    message = features_mes;
                    if(!clients.containsKey(request.getAddress()))
                        clients.put(request.getAddress(), 0);
                    else{
                        clients.remove(request.getAddress());
                        clients.put(request.getAddress(), 0);
                    }
                }else{
                    //handle requests                   
                    switch(clients.get(request.getAddress())){
                        case 0:
                            try{
                                int choice = Integer.parseInt(input);
                                if (choice < 1 || choice > 8)
                                    throw new Exception("Invalid choice");
                                switch(choice){
                                    case 1:
                                        message = create_account();
                                        clients.remove(request.getAddress());
                                        clients.put(request.getAddress(), 1);
                                        break;
                                    case 2:
                                        message = close_account();
                                        clients.remove(request.getAddress());
                                        clients.put(request.getAddress(), 2);
                                        break;
                                    case 3:
                                        message = deposit();
                                        clients.remove(request.getAddress());
                                        clients.put(request.getAddress(), 3);
                                        break;
                                    case 4:
                                        message = withdraw();
                                        clients.remove(request.getAddress());
                                        clients.put(request.getAddress(), 4);
                                        break;
                                    case 5:
                                        message = transfer();
                                        clients.remove(request.getAddress());
                                        clients.put(request.getAddress(), 5);
                                        break;
                                    case 6:
                                        message = transactions();
                                        clients.remove(request.getAddress());
                                        clients.put(request.getAddress(), 6);
                                        break;    
                                    case 7:
                                        message = monitor();
                                        clients.remove(request.getAddress());
                                        clients.put(request.getAddress(), 7);
                                    case 8:
                                        message = "bye";
                                        System.out.println("release connection ip: " + request.getAddress().toString());
                                        clients.remove(request.getAddress());
                                        break;
                                }
                            }
                            catch(NumberFormatException e) {
                                message = "Error: " + e.getMessage();
                                finish_trans = true;
                            }
                            catch(Exception e){
                                message = "Error: " + e.getMessage();
                                finish_trans = true;
                            }                                
                            break;
                        case 1:
                            finish_trans = true;
                            try{
                                if (inputs.length != 4)
                                    throw new Exception("Invalid parameters");
                                String name = inputs[0];
                                String password = inputs[1];
                                if (password.length() != 8)
                                    throw new Exception("Password Length must be 8");
                                
                                boolean match = false;
                                for(Currency e : Currency.values()){
                                    if (e.name().equals(inputs[2]))
                                        match = true;
                                }
                                if (!match)
                                    throw new Exception("Invalid currency type");
                                Currency type = Currency.valueOf(inputs[2]);
                              
                                
                                float balance = Float.parseFloat(inputs[3]);
                                if (balance < 0)
                                    throw new Exception("Negative balance");
                                
                                message = create_account(name,password,type,balance);
                            }catch(Exception e){
                                message = "Error: Wrong request " + e.getMessage();
                            }
                            break;
                        case 2:
                            finish_trans = true;
                            try{
                                if (inputs.length != 3)
                                    throw new Exception("Invalid parameters");
                                String name = inputs[0];
                                String password = inputs[1];                            
                                int account_number = Integer.parseInt(inputs[2]);
                                message = close_account(name,password,account_number);
                            }catch(Exception e){
                                message = "Error: Wrong request " + e.getMessage();
                            }
                            break;
                        case 3:
                            finish_trans = true;
                            try{
                                if (inputs.length != 5)
                                    throw new Exception("Invalid parameters");
                                int acc_no = Integer.parseInt(inputs[0]);
                                String name = inputs[1];
                                String password = inputs[2];                               
                                boolean match = false;
                                for(Currency e : Currency.values()){
                                    if (e.name().equals(inputs[3]))
                                        match = true;
                                }
                                if (!match)
                                    throw new Exception("Invalid currency type");
                                Currency type = Currency.valueOf(inputs[3]);                                                            
                                float amount = Float.parseFloat(inputs[4]); 
                                
                                message = deposit(acc_no,name,password,type,amount);
                            }catch(Exception e){
                                message = "Error: Wrong request " + e.getMessage();
                            }
                            break;
                        case 4:
                            finish_trans = true;
                            try{
                                if (inputs.length != 5)
                                    throw new Exception("Invalid parameters");
                                int acc_no = Integer.parseInt(inputs[0]);
                                String name = inputs[1];
                                String password = inputs[2];                               
                                boolean match = false;
                                for(Currency e : Currency.values()){
                                    if (e.name().equals(inputs[3]))
                                        match = true;
                                }
                                if (!match)
                                    throw new Exception("Invalid currency type");
                                Currency type = Currency.valueOf(inputs[3]);                                                            
                                float amount = Float.parseFloat(inputs[4]); 
                                
                                message = withdraw(acc_no,name,password,type,amount);
                            }catch(Exception e){
                                message = "Error: Wrong request " + e.getMessage();
                            }
                            break;
                        case 5:
                            finish_trans = true;
                            try{
                                if (inputs.length != 7)
                                    throw new Exception("Invalid parameters");
                                int acc_no1 = Integer.parseInt(inputs[0]);
                                String name1 = inputs[1];
                                String password1 = inputs[2];                               
                                boolean match = false;
                                for(Currency e : Currency.values()){
                                    if (e.name().equals(inputs[3]))
                                        match = true;
                                }
                                if (!match)
                                    throw new Exception("Invalid currency type");
                                Currency type = Currency.valueOf(inputs[3]); 
                                int acc_no2 = Integer.parseInt(inputs[4]);
                                String name2 = inputs[5];
                                float amount = Float.parseFloat(inputs[6]); 
                                
                                message = transfer(acc_no1,name1,password1,type,acc_no2,name2,amount);
                            }catch(Exception e){
                                message = "Error: Wrong request " + e.getMessage();
                            }
                            break;
                        case 6:
                            finish_trans = true;
                            try{
                                if (inputs.length != 3)
                                    throw new Exception("Invalid parameters");
                                int acc_no = Integer.parseInt(inputs[0]);
                                String name = inputs[1];
                                String password = inputs[2];                               
                                
                                message = transactions(acc_no,name,password);
                            }catch(Exception e){
                                message = "Error: Wrong request " + e.getMessage();
                            }
                            break;
                        case 7:
                            finish_trans = true;
                            try{
                                if (inputs.length != 1)
                                    throw new Exception("Invalid parameters");
                                int duration = Integer.parseInt(inputs[0]);                                                             
                                message = monitor(request.getAddress(),request.getPort(),duration);
                            }catch(Exception e){
                                message = "Error: Wrong request " + e.getMessage();
                            }
                            break;
                    }
                }
                
                //return reply to user
                if(finish_trans){
                    message = message + features_mes;
                    clients.remove(request.getAddress());
                    clients.put(request.getAddress(), 0);
                }
                DatagramPacket reply = new DatagramPacket(message.getBytes(),message.length(),request.getAddress(),request.getPort());
                int ran=(int)(Math.random()*10);
                if (ran >2 ) {
                    socket.send(reply);
                } else {
                    System.out.println("Reply lost!");
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
        System.out.println("\ncreate new account:"
                + "\nnumber: " + acc_no
                + "\nname: " + name
                + "\npassword: " + password
                + "\ntype: " + type.name()
                + "\nbalance: " + balance);
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
        System.out.println("\nclose account:"
                + "\nnumber: " + account_number
                + "\nname: " + name
                + "\npassword: " + password);
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
        System.out.println("\nupdate balance"
                + "\nnumber: " + account_number
                + "\nname: " + name
                + "\npassword: " + password
                + "\nold balance: " + (account.get_balance() - amount)
                + "\nnew balance: " + account.get_balance());
        Date date = new Date();
        account.add_event(amount, new Timestamp(date.getTime()));
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
        System.out.println("\nupdate balance"
                + "\nnumber: " + account_number
                + "\nname: " + name
                + "\npassword: " + password
                + "\nold balance: " + (account.get_balance() + amount)
                + "\nnew balance: " + account.get_balance());
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
        System.out.println("\ntransfer"
                + "\nnumber: " + account_number1
                + "\nname: " + name1
                + "\npassword: " + password
                + "\nold balance: " + (account1.get_balance() + amount)
                + "\nnew balance: " + account1.get_balance()
                + "\n to number: " + account_number2
                + "\n name: " + name2
                + "\nold balance: " + (account2.get_balance() - amount)
                + "\nnew balance: " + account2.get_balance());
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
        System.out.println("\ncheck transaction"
                + "\nnumber: " + account_number
                + "\nname: " + name
                + "\npassword: " + password);
        return account.get_events();
    }
    
    /**
     * monitor
     */
    private static String monitor(){
        return "Specify monitoring duration in seconds";
    }
    
    private static String monitor(InetAddress address, int port, int duration){
        Identity monitor = new Identity(address,port);
        if(!monitorings.containsKey(monitor)){
            Date date = new Date();
            int end = (int) (date.getTime() + duration * 1000);
            Date end_date = new Date(end);
            monitorings.put(monitor, end_date);
        }
        return "Start monitoring";
    }
}