/**
 * 
 */
package lib;

import java.io.Serializable;

public class AppendEntriesReply implements Serializable{

    private static final long serialVersionUID = 1L;
    private int term;
    private boolean success;
    
    public AppendEntriesReply(int term, boolean success) {
        // TODO Auto-generated constructor stub
        this.term = term;
        this.success = success;
    }

    /**
     * @return the term
     */
    public int getTerm() {
        return term;
    }

    /**
     * @param term the term to set
     */
    public void setTerm(int term) {
        this.term = term;
    }

    /**
     * @return the success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @param success the success to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "AppendEntriesReply: term " + term + " success " + success;
    }
}
