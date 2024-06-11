/**
 * The PingServerFactory will create a pingServer based on the given
 * host name and post number and return it
 */
package pingpong;

import java.net.InetSocketAddress;

import rmi.RMIException;
import rmi.Stub;

public class PingServerFactory {

    public static pingpongInterface makePingServer(String hostname, int port) throws RMIException{
        InetSocketAddress socketAddress = new InetSocketAddress(hostname, port);
        pingpongInterface pingServer = Stub.create(pingpongInterface.class, socketAddress);
        return pingServer;
    }

}
