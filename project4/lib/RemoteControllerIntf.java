
package lib;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteControllerIntf extends Remote {
    //you can add the same methods as you define in MessageHandlingIntf.
    //send Message is an example
    Message deliverMessage(Message message) throws RemoteException;
    GetStateReply getState() throws RemoteException;
    byte[] mineNewBlock(byte[] data) throws RemoteException;
    void broadcastNewBlock() throws RemoteException;
    void downloadChain() throws RemoteException;
}
