/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bankingsystem;

import java.sql.Timestamp;
import java.util.ArrayList;
/**
 *
 * @author Quan Hoang
 */
public class BankAccount{
    private static int number_of_accounts = 0;
    private int account_number;
    private String name;
    private String password = new String(new char[8]);
    Currency type;
    private float balance;
    private ArrayList<Transaction> transactions;
    
    private BankAccount(){
    }
    
    public BankAccount(String name, String password, Currency type, float balance){
        this.account_number = ++number_of_accounts;
        this.name = name;
        this.password = password;
        this.type = type;
        this.balance = balance;
        transactions = new ArrayList<>();
    }
    
    public int get_account_number(){
        return account_number;
    }
    
    public String get_name(){
        return name;
    }
    
    public String get_password(){
        return password;
    }
    
    public float get_balance(){
        return balance;
    }
    
    public Currency get_type(){
        return type;
    }
    
    public void set_balance(float balance){
        this.balance = balance;
    }
    
    public String get_events(){
        String result = "";
        for(Transaction e: transactions){
            String action;
            if (e.get_amount() < 0)
                action = "Money Out: " + Float.toString(e.get_amount());
            else
                action = "Money In: " + Float.toString(Math.abs(e.get_amount()));
            result = result + action + " on " + e.get_time().toString() + "\n";
        }
        return result;
    }
    
    public void add_event(float amount, Timestamp time){
        transactions.add(new Transaction(amount,time));
    }
    
    class Transaction{
        private float amount;
        private Timestamp time;
        
        public Transaction(float amount, Timestamp time){
            this.amount = amount;
            this.time = time;
        }
        
        public float get_amount(){
            return amount;
        }
        
        public Timestamp get_time(){
            return time;
        }
    }
}

enum Currency{
    SGD, USD, VND
}
