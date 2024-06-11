package lib;

import java.io.Serializable;

/**
 * This is a serializable packet wrapper for the getState method of
 * transportLayer.
 *
 * @author Sphoorti Joglekar, Priya Avhad, Yijia Cui, Zonglin Wang
 */
public class GetStateReply implements Serializable {
    /**
     * The current term number.
     */
    public int length;
    /**
     * The leader flag.
     */
    public String lastHash;

    private static final long serialVersionUID = 1L;

    /**
     * GetStateReply - Construct a get state packet with given term and leader.
     *
     * @param length the current term
     * @param lastHash The leader flag
     */
    public GetStateReply(int length, String lastHash) {
        this.length = length;
        this.lastHash = lastHash;
    }
}
