package lib;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller.
 */
public class Controller extends UnicastRemoteObject implements MessageLayer{
    /**
     * For byte serialization.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Registry for the RMI server.
     */
    private Registry registry;
    /**
     * Contains map from the node ID to the remote reference object of each
     * raft peer.
     */
    private Map<Integer, InnerNode> nodes;

    private Map<Integer, InnerNode> disconnectNodes;

    /**
     * Controller - Construct a controller listening on a given port.
     * @param port the given port for controller
     * @throws Exception
     */
    public Controller(int port) throws Exception{
        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (RemoteException e) {
            //already exist
            System.out.println("already exist!");
        }
        Naming.rebind("rmi://localhost:" + port + "/MessageServer", this);
        nodes = new ConcurrentHashMap<>();
        disconnectNodes = new ConcurrentHashMap<>();
    }

    public void re_connect(int id){
        if(disconnectNodes.containsKey(id)) {
            nodes.put(id, disconnectNodes.get(id));
            disconnectNodes.remove(id);
        }
    }

    public void disconnect(int id){
        if( disconnectNodes.containsKey(id) ) { /* Already disconnected. */
            return;
        }

        disconnectNodes.put(id, nodes.get(id));
        nodes.remove(id);
    }


    /**
     * register - Register the node with the Message Server.
     * @param id the node id
     * @param remoteController the remoteControllerInterface of a raft peer.
     */
    public void register(int id, RemoteControllerIntf remoteController) {
        this.nodes.putIfAbsent(id, new InnerNode (id, remoteController));
    }


    /**
     * send - Start a new thread to send out the message.
     * @param message the message to be sent.
     *
     * @throws RemoteException when the deliver RMI call fails.
     */
    public Message send(Message message) throws RemoteException {
        Message reply = null;
        InnerNode n = nodes.get(message.getDest());
        if (n != null) {
            //the controller calls remoteController deliverMessage.
            reply = n.rc.deliverMessage(message);
        }
        return reply;
    }

    /**
     * getNumRegistered - return the currently registered nodes.
     *
     * @return the number of registered nodes.
     */
    public int getNumRegistered() {
        return this.nodes.size();
    }

    /**
     * getState - This is the remote call for getting the node state.
     *
     * @param nodeID the node ID.
     * @return the state information packet
     */
    public GetStateReply getState( int nodeID ) {
        GetStateReply reply;

        try {
            InnerNode n = nodes.get(nodeID);
            if (n != null) {
                reply = n.rc.getState();
                return reply;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit( -1 );
        }

        return null;
    }

    /**
     * This is the remote call for mining a new block.
     *
     * @param nodeID the node ID.
     * @return the state information packet
     */
    public byte[] mineNewBlock( int nodeID , byte[] data) {
        byte[] blockData = null;

        try {
            InnerNode n = nodes.get(nodeID);
            if (n != null) {
                blockData = n.rc.mineNewBlock(data);
                return blockData;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit( -1 );
        }

        return null;
    }

    public void broadcastNewBlock( int nodeID ) {
        byte[] data = null;
        try {
            InnerNode n = nodes.get(nodeID);
            if (n != null) {
                System.out.println("Transport send broadcastNewBlock to peer:" + nodeID);
                n.rc.broadcastNewBlock();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit( -1 );
        }

        return;
    }

    public void downloadChain(int nodeID) {
        try {
            InnerNode n = nodes.get(nodeID);
            if (n != null) {
                System.out.println("Transport send downloadChain to peer:" + nodeID);
                n.rc.downloadChain();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit( -1 );
        }

        return;
    }


    /**
     * Node - Class to represent a raft node from the serer point of view.
     */
    private static class InnerNode {
        int id;
        RemoteControllerIntf rc;
        InnerNode (int id, RemoteControllerIntf rc) {
            this.id = id;
            this.rc = rc;
        }
    }
}
