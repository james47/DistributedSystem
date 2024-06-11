package lib;

import java.io.Serializable;
/**
 * This class is a wrapper for packing all the result information that you
 * might use in your own implementation of the RequestVote call, and also
 * should be serializable to return by remote function call.
 *
 */
public class RequestVoteReply implements Serializable{
    private static final long serialVersionUID = 1L;
    private int term;
    private boolean voteGranted;
    public RequestVoteReply(boolean gotVote) {
        this.voteGranted = gotVote;
    }
    /**
     * @return the voteGranted
     */
    public boolean isVoteGranted() {
        return voteGranted;
    }
    /**
     * @param voteGranted the voteGranted to set
     */
    public void setVoteGranted(boolean gotVote) {
        this.voteGranted = gotVote;
    }
    
}
