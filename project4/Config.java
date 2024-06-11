import lib.Block;
import lib.Controller;
import lib.GetStateReply;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class Config extends UnicastRemoteObject implements Remote {

    private static final long serialVersionUID = 1L;

    /* ##### Controller ##### */
    private int controllerPort = 14736;   /* Port on which the controller is listening. */
    private Controller transportLayerCtrl;  /* Transport layer controller for this project. */

    private int numServers;         /* Number of servers in this network. */
    private Process blockchainPeers[];    /* Process for Raft server. */
    private boolean connected[];    /* Whether each server is connected to this network. */

    private Process spawnRaftPeer( int controllerPort, int id, int numServers ) {

        Process raftPeer = null;
        List<String> commands = new ArrayList<String>();
        commands.add("java");
        commands.add("Node");
        commands.add(String.valueOf(controllerPort));
        commands.add(String.valueOf(id));
        commands.add(String.valueOf(numServers));

        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.inheritIO();

        try {
            raftPeer = builder.start();
        } catch (Exception e) {
            e.printStackTrace();
            cleanup();
        }

        return raftPeer;
    }

    /* Creates a configuration to be used by a tester or a service. */
    public Config( int numServers, int ctrlPort ) throws RemoteException {

        /* Setup the transport layer controller. */
        controllerPort = ctrlPort;
        try {
            transportLayerCtrl = new Controller(controllerPort);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit( -1 );
        }

        this.numServers = numServers;

        connected = new boolean[numServers];
        blockchainPeers = new Process[numServers];
        for( int i = 0; i < numServers; i++ ) {

            /* Create a new Raft server. */
            blockchainPeers[i] = spawnRaftPeer(controllerPort, i, numServers);

            /* Connect this raft peer*/
            this.connect(i);
        }
    }

    /* Attach server "whichServer" to this config's network. */
    public void connect( int whichServer ) throws RemoteException{
        this.connected[whichServer] = true;
        transportLayerCtrl.re_connect(whichServer);
    }

    /* Detach server "whichServer" from this config's network. */
    public void disconnect( int whichServer ) throws RemoteException{
        System.out.println("disconnect this " + whichServer);
        this.connected[whichServer] = false;
        transportLayerCtrl.disconnect(whichServer);
    }

    public void cleanup() {

        for(int i = 0; i < numServers; i++) {

            if( blockchainPeers[i] != null ) {
                blockchainPeers[i].destroy();
            }
        }
        System.exit( 0 );
    }

    public void waitUntilAllRegister() {

        int numRegistered = 0;
        while( numRegistered != this.numServers ) {

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                cleanup();
            }

            numRegistered = transportLayerCtrl.getNumRegistered();
        }
    }


    public GetStateReply getState(int nodeID ) {
        return transportLayerCtrl.getState(nodeID);
    }

    public Block sendMiningRequestToNode(int nodeID, byte[] data) {
        byte[] retData = transportLayerCtrl.mineNewBlock(nodeID, data);
        return Block.fromString(new String(retData));
    }

    public void broadcastNewBlock(int nodeID) {
        transportLayerCtrl.broadcastNewBlock(nodeID);
    }

    public void downloadChain(int nodeID) {
        transportLayerCtrl.downloadChain(nodeID);
    }
}