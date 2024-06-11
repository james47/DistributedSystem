package lib;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteController extends UnicastRemoteObject implements RemoteControllerIntf {
    /**
     * Delegates
     */
    public final MessageHandling message_callback;
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * @param mh the messageHandling object that takes the responsibility.
     * @throws RemoteException RemoteException thrown by RMI.
     */
    public RemoteController(MessageHandling mh) throws RemoteException {
        this.message_callback = mh;
    }

    /**
     * Delegates the deliverMessage to the MessageHandling object.
     * @param message the message sent to the node.
     * @return the reply of the processed message.
     * @throws RemoteException RemoteException thrown by RMI.
     */
    public Message deliverMessage(Message message) throws RemoteException {
        return message_callback.deliverMessage(message);
    }

    public GetStateReply getState() throws RemoteException{
        return message_callback.getState();
    }

    @Override
    public byte[] mineNewBlock(byte[] data) throws RemoteException {
        return message_callback.mineNewBlock(data);
    }

    @Override
    public void broadcastNewBlock() throws RemoteException {
        message_callback.broadcastNewBlock();
    }

    @Override
    public void downloadChain() throws RemoteException {
        message_callback.downloadChain();
    }
}
