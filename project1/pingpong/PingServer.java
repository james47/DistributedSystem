/**
 * A simple PingPong Server
 * when a client calls the method ping(id)
 * the method call will be forward to the server
 * and server will return a string:
 * "Pong" + id
 * 
 */
package pingpong;

import java.net.InetSocketAddress;
import java.util.Scanner;

import rmi.RMIException;
import rmi.Skeleton;

public class PingServer implements pingpongInterface{
    
    // hard code the host name and the port number
    public static final String HOSTNAME = "unix4.andrew.cmu.edu";
    public static final int PORT = 34329;

    public static void main(String[] args) throws RMIException {
        // create a skeleton and start it
        pingpongInterface pingServer = new PingServer();
        InetSocketAddress socketAddress = new InetSocketAddress(HOSTNAME, PORT);
        Skeleton<pingpongInterface> skeleton = new Skeleton(pingpongInterface.class, pingServer, socketAddress);
        skeleton.start();
        
        // wait for the user to terminate the server
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type 'S' to stop server...");
        while (!scanner.nextLine().equals("S")) {
            System.out.println("Type 'S' to stop server...");
        }
        skeleton.stop();
    }

    @Override
    public String ping(int idNumber) {
        String reply = "Pong" + idNumber;
        return reply;
    }

}
