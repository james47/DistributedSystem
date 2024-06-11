package lib;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MessageLayer extends Remote {
    void register(int id, RemoteControllerIntf remoteController) throws RemoteException;
    Message send(Message message) throws RemoteException;
}
