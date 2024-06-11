import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.math.BigInteger;

import lib.Block;

public class BlockChain implements BlockChainBase {
    
    private List<Block> blockChain;
    private int difficulty;
    private Node node;
    private Block tempBlock;
    private final Object lock = new Object();
    /**
     * 
     */
    public BlockChain(Node node, int difficulty) {
        this.blockChain = new ArrayList<Block>();
        this.node = node;
        setDifficulty(difficulty);
        blockChain.add(createGenesisBlock());
    }

    /* (non-Javadoc)
     * @see BlockChainBase#addBlock(lib.Block)
     */
    @Override
    public boolean addBlock(Block block) {
        synchronized (lock) {
            Block prevBlock = getLastBlock();
            // check validity of new block
            if (!isValidNewBlock(block, prevBlock))
                return false;
            // multiple new block
            if (prevBlock.getPreviousHash().equals(block.getPreviousHash())) {
                if (prevBlock.getTimestamp() > block.getTimestamp()) {
                    blockChain.remove(blockChain.size() - 1);
                    return blockChain.add(block);
                }
                else
                    return false;
            }
            // validation
            else if (!prevBlock.getHash().equals(block.getPreviousHash()))
                return false;
            return blockChain.add(block);
        }
    }

    /* (non-Javadoc)
     * @see BlockChainBase#createGenesisBlock()
     */
    @Override
    public Block createGenesisBlock() {
        String hash = "0000000000";
        String prevHash = "0000000001";
        String data = "Genesis Block";
        long timestamp = new Date().getTime();
        Block genesisBlock = new Block(hash, prevHash, data, timestamp);
        genesisBlock.setDifficulty(difficulty);
        genesisBlock.setNonce(0);
        return genesisBlock;
    }

    /* (non-Javadoc)
     * @see BlockChainBase#createNewBlock(java.lang.String)
     */
    @Override
    public byte[] createNewBlock(String data) {
        Block prevBlock = getLastBlock();
        String prevHash = prevBlock.getHash();
        String str = data + prevHash;
        int nonce = 0;
        String newHash = "";
        long timestamp;
        while (true) {
            timestamp = new Date().getTime();
            String newstr = str + timestamp + nonce;
            newHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(newstr);
            if (isPrefixValid(newHash, difficulty))
                break;
            nonce++;
        }
        Block newBlock = new Block(newHash, prevHash, data, timestamp);
        newBlock.setDifficulty(difficulty);
        newBlock.setNonce(nonce);
        tempBlock = newBlock;
        //return Serializer.serialize(newBlock);
        return newBlock.toString().getBytes();
    }

    /* (non-Javadoc)
     * @see BlockChainBase#broadcastNewBlock()
     */
    @Override
    public boolean broadcastNewBlock() {
        int peerNum = node.getPeerNumber();
        int votes = 0;
        byte[] blockData = tempBlock.toString().getBytes();
        for (int i = 0; i < peerNum; i++) {
            try {
                if (node.broadcastNewBlockToPeer(i, blockData))
                    votes++;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return votes == peerNum;
    }

    /* (non-Javadoc)
     * @see BlockChainBase#setDifficulty(int)
     */
    @Override
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    /* (non-Javadoc)
     * @see BlockChainBase#getBlockchainData()
     */
    @Override
    public byte[] getBlockchainData() {
        return Serializer.serialize(blockChain);
    }

    /* (non-Javadoc)
     * @see BlockChainBase#downloadBlockchain()
     */
    @Override
    public void downloadBlockchain() {
        int peerNum = node.getPeerNumber();
        List<Block> currentBlockChain = new ArrayList<Block>();
        for (int i = 0; i < peerNum; i++) {
            byte[] bcData;
            try {
                bcData = node.getBlockChainDataFromPeer(i);
                List<Block> bc = (List<Block>) Serializer.deserialize(bcData);
                if (bc.size() > currentBlockChain.size())
                    currentBlockChain = new ArrayList<Block>(bc);
                else if (bc.size() == currentBlockChain.size()) {
                    if (bc.get(bc.size() - 1).getTimestamp() > 
                        currentBlockChain.get(currentBlockChain.size() - 1).getTimestamp()) {
                        currentBlockChain = new ArrayList<Block>(bc);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.blockChain = new ArrayList<Block>(currentBlockChain);
    }

    /* (non-Javadoc)
     * @see BlockChainBase#setNode(Node)
     */
    @Override
    public void setNode(Node node) {
        this.node = node;
    }

    /* (non-Javadoc)
     * @see BlockChainBase#isValidNewBlock(lib.Block, lib.Block)
     */
    @Override
    public boolean isValidNewBlock(Block newBlock, Block prevBlock) {
        if (!newBlock.getPreviousHash().equals(prevBlock.getHash()))
            return false;
        String str = newBlock.getData() + newBlock.getPreviousHash() + newBlock.getTimestamp() + newBlock.getNonce();
        String hash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(str);
        return hash.equals(newBlock.getHash());
    }

    /* (non-Javadoc)
     * @see BlockChainBase#getLastBlock()
     */
    @Override
    public Block getLastBlock() {
        return blockChain.get(blockChain.size() - 1);
    }

    /* (non-Javadoc)
     * @see BlockChainBase#getBlockChainLength()
     */
    @Override
    public int getBlockChainLength() {
        return blockChain.size();
    }

    private boolean isPrefixValid(String hash, int difficulty) {
        String value = new BigInteger(hash, 16).toString(2);
        String formatPad = "%" + (hash.length() * 4) + "s";
        String binHash = String.format(formatPad, value).replace(" ", "0");
        for (int i = 0; i < difficulty; ++i) {
            if (binHash.charAt(i) != '0')
                return false;
        }
        return true;
    }
}
