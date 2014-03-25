/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bankingsystem;


/**
 *
 * @author Quan Hoang
 */
public class BankingSystem {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        String[] arguments = new String[1];
        String[] argss = new String[1];
        argss[0] = "1";
        arguments[0] = "localhost";
        Server s = new Server();s.main(new String[0]);
        Client c = new Client();c.main(arguments);
    }
    
}
