package lib;

import java.io.Serializable;


/**
 * Block Class, the element to compose a Blockchain.
 */
public class Block implements Serializable{

    private static final long serialVersionUID = 1L;
    private String hash;

    private String previousHash;

    private String data;

    private long timestamp;

    private int difficulty;

    private long nonce;

    public Block() {}

    public Block(String hash, String previousHash, String data,
                 long timestamp) {
        this.hash = hash;
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = timestamp;
    }


    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String toString() {
        String str = "" + previousHash + "," + hash + "," + data + "," + timestamp + "," + difficulty + "," + nonce;
        return str;
    }
    
    public static Block fromString(String s){
        String[] strArr = s.split(",");
        String prevHash = strArr[0];
        String hash = strArr[1];
        String data = strArr[2];
        long timestamp = Long.parseLong(strArr[3]);
        int difficulty = Integer.parseInt(strArr[4]);
        long nonce = Long.parseLong(strArr[5]);
        Block newBlock = new Block(hash, prevHash, data, timestamp);
        newBlock.setDifficulty(difficulty);
        newBlock.setNonce(nonce);
        return newBlock;
    }

}
