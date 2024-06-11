import lib.*;

import java.rmi.RemoteException;

/**
 * This class is for the distributed miner node, each Node is a client to for
 * mining new blocks and add them into the peer-to-peer blockchain network.
 */
public class Node implements MessageHandling {
    private int id;
    private static TransportLib lib;
    private int num_peers;
    private int wallet;
    BlockChain blockChainManager;
    private final Object lock = new Object();

    /**
     * Node constructor
     * @param port the server port
     * @param id the id of the node
     * @param num_peers number of peers in the network.
     */
    public Node(int port, int id, int num_peers) {
        this.id = id;
        lib = new TransportLib(port, id, this);
        wallet = 0;
        this.num_peers = num_peers;
        //TODO: instantiate your blockchain implementation and set the difficulty to 20.
        blockChainManager = new BlockChain(this, 20);
    }

    /**
     * Deliver message. All the requests should be handling here.
     * @param message delivered message
     * @return reply for the message.
     */
    public Message deliverMessage(Message message) {
        byte[] data = null;
        MessageType type = null;
        Block newBlock = null;
        switch (message.getType()) {
            // for blockchain to use
            case ON_BROADCAST_NEW_BLOCK:
                data = message.getBody();
                newBlock = Block.fromString(new String(data));
                boolean agree = blockChainManager.addBlock(newBlock);
                if(agree) {
                    type = MessageType.AGREE_BROADCAST_NEW_BLOCK;
                }
                else
                    type = MessageType.DISAGREE_BROADCAST_NEW_BLOCK;
                break;
            case GET_BLOCKCHAIN_DATA:
                type = MessageType.GET_BLOCKCHAIN_DATA;
                data = blockChainManager.getBlockchainData();
                break;
            default:
        }

        Message reply = new Message(type, message.getDest(),
                message.getSrc(), data);
        return reply;
    }

    /**
     * This method is for BlockChainBase implementation.
     * You will need to call this method when you want to download the
     * block chain from your peer.
     * @param peerAddr the peer address.
     * @return the byte representation of the blockchain. You need a way to
     * convert between the byte representation and a block chain.
     */
    public byte[] getBlockChainDataFromPeer(int peerAddr)
            throws RemoteException {
        Message m = new Message(MessageType.GET_BLOCKCHAIN_DATA,
                id, peerAddr, null);
        Message reply = lib.sendMessage(m);
        return reply.getBody();
    }

    /**
     * This method is used to broadcast the new block to the node's peer.
     * You may need to call this method from your broadcastNewBlock inside
     * the BlockChainBase implementation.
     * @param peerAddr the address of your peer.
     * @param blockData the byte representation of the new block.
     * @return whether the new block agree or disagree to add this block.
     */
    public boolean broadcastNewBlockToPeer(int peerAddr, byte[] blockData)
            throws RemoteException {
        Message m = new Message(MessageType.ON_BROADCAST_NEW_BLOCK,
                id, peerAddr, blockData);
        Message reply = lib.sendMessage(m);

        if(reply == null)
            return true;

        if(reply.getType() == MessageType.AGREE_BROADCAST_NEW_BLOCK)
            return true;
        else if(reply.getType() == MessageType.DISAGREE_BROADCAST_NEW_BLOCK) {
            return false;
        }
        else{
            System.out.println("wrong reply type!");
            return false;
        }
    }

    /**
     * Get the number of nodes in the network
     * @return the number of nodes.
     */
    public int getPeerNumber(){
        return num_peers;
    }

    /************************ Test usage *****************************/

    /**
     * Get the length of the block chain and also the hash for the
     * latest block in the chain
     * @return the reply packet containing the required information.
     */
    @Override
    public GetStateReply getState() {
        synchronized (lock) {
            int l = blockChainManager.getBlockChainLength();
            Block last = blockChainManager.getLastBlock();
            return new GetStateReply(l, last.getHash());
        }
    }

    /**
     * Mine the new block based on the data given
     * @param data the byte representation for the string data.
     * @return byte representation of the new block mined.
     */
    @Override
    public byte[] mineNewBlock(byte[] data) {
        return blockChainManager.createNewBlock(new String(data));
    }

    /**
     * broadcast new block
     * The test will call this method to require the node to broadcast the new block
     * to all its peers.
     */
    @Override
    public void broadcastNewBlock() {
        if(blockChainManager.broadcastNewBlock()) {
            wallet++;
            System.out.println("Node " + id + " get a coin, " +
                    wallet + " total");
        }
    }

    /**
     * To download the blockchain from its peers.
     * This is used by the test to force the node to download the
     * blockchain from its peer.
     */
    @Override
    public void downloadChain() {
        blockChainManager.downloadBlockchain();
    }

    //main function
    public static void main(String args[]) throws Exception {
        if (args.length != 3) throw new Exception("Need 3 args: <port> <id> <num_peers>");
        Node UN = new Node(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
    }
}
