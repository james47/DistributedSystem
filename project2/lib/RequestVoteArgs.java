package lib;

import java.io.Serializable;

/**
 * This class is a wrapper for packing all the arguments that you might use in
 * the RequestVote call, and should be serializable to fill in the payload of
 * Message to be sent.
 *
 */
public class RequestVoteArgs implements Serializable{

    private static final long serialVersionUID = 1L;
    private int candidateTerm;
    private int candidateId;
    private int lastLogTerm;
    private int lastLogIndex;
    public RequestVoteArgs(int candidateId, int candidateTerm, int lastLogTerm, int lastLogIndex) {
        this.candidateId = candidateId;
        this.candidateTerm = candidateTerm;
        this.lastLogTerm = lastLogTerm;
        this.lastLogIndex = lastLogIndex;
    }
    /**
     * @return the candidateTerm
     */
    public int getCandidateTerm() {
        return candidateTerm;
    }
    /**
     * @param candidateTerm the candidateTerm to set
     */
    public void setCandidateTerm(int candidateTerm) {
        this.candidateTerm = candidateTerm;
    }
    /**
     * @return the candidateId
     */
    public int getCandidateId() {
        return candidateId;
    }
    /**
     * @param candidateId the candidateId to set
     */
    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }
    /**
     * @return the lastLogTerm
     */
    public int getLastLogTerm() {
        return lastLogTerm;
    }
    /**
     * @param lastLogTerm the lastLogTerm to set
     */
    public void setLastLogTerm(int candidateTerm) {
        this.lastLogTerm = candidateTerm;
    }
    /**
     * @return the lastLogIndex
     */
    public int getLastLogIndex() {
        return lastLogIndex;
    }
    /**
     * @param lastLogIndex the lastLogIndex to set
     */
    public void setLastLogIndex(int candidataIndex) {
        this.lastLogIndex = candidataIndex;
    }
    
}
