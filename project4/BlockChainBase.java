import lib.Block;

/**
 * This interface define the APIs to implement a BlockChain layer to
 * provide necessary support for application requirement.
 */
public interface BlockChainBase {

    /**
     * Add a new block into the blockchain.
     * @param block the block to be added.
     * @return whether the block is accepted or not.
     */
    boolean addBlock(Block block);

    /**
     * Create the hardcoded first block in the chain.
     * @return the first block.
     */
    Block createGenesisBlock();

    /**
     * Create a block based on the string data.
     * @param data the data contained in the block
     * @return the byte representation of the block.
     */
    byte[] createNewBlock(String data);

    /**
     * broadcast the new block to all the peer in the network
     * @return whether the node is added into the chain or not.
     */
    boolean broadcastNewBlock();

    /**
     * Set the difficulty of the proof-of-work
     * @param difficulty the difficulty
     */
    void setDifficulty(int difficulty);

    /**
     * get the byte representation of the blockchain
     * @return the byte representation
     */
    byte[] getBlockchainData();

    /**
     * Download the blockchain from its peer.
     */
    void downloadBlockchain();

    /**
     * Set the node for communication
     * @param node the node class.
     */
    void setNode(Node node);

    /**
     * Validate the new block based on the block before that.
     * @param newBlock the new block
     * @param prevBlock the block before that.
     * @return valid or not
     */
    boolean isValidNewBlock(Block newBlock, Block prevBlock);

    /**
     * Get the latest block added into the chain
     * @return the latest block.
     */
    Block getLastBlock();

    /**
     * Get the length of the block chain
     * @return the sie of blockchain
     */
    int getBlockChainLength();
}
