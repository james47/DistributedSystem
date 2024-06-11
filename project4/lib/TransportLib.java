package lib;

import java.rmi.Naming;
import java.rmi.RemoteException;

public class TransportLib {
    /**
     * id of the node.
     */
    private int id;
    /**
     * The MessageLayer exported by RMI.
     */
    private MessageLayer messageLayer;

    /**
     * The remote controller client object
     */
    private RemoteControllerIntf remoteController;

    /**
     * Transport Lib.
     * @param port port of the server
     * @param id id of the node
     * @param messageHandling the implemented MessageHandling object.
     */
    public TransportLib(int port, int id, MessageHandling messageHandling) {
        try {
            this.remoteController = new RemoteController(messageHandling);
            messageLayer = (MessageLayer) Naming.lookup("rmi://localhost:" + port + "/MessageServer");
            messageLayer.register(id, remoteController);
        } catch(Exception e) {
            System.out.println(port);
            e.printStackTrace();
            System.exit(-1);
        }
        this.id = id;
    }

    /**
     * Send message through message server.
     * @param message
     * @throws RemoteException
     */
    public Message sendMessage(Message message) throws RemoteException {
        return messageLayer.send(message);
    }
}
