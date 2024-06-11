/**
 * 
 */
package lib;

import java.io.Serializable;

public class AppendEntriesArgs implements Serializable{

    private static final long serialVersionUID = 1L;
    private int term;
    private int leaderId;
    private int prevLogIndex;
    private int prevLogTerm;
    private LogEntries[] entries;
    private int leaderCommit;
    public AppendEntriesArgs(int term, int leaderId, int prevLogIndex, int prevLogTerm, LogEntries[] entries,
            int leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
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
     * @return the leaderId
     */
    public int getLeaderId() {
        return leaderId;
    }
    /**
     * @param leaderId the leaderId to set
     */
    public void setLeaderId(int leaderId) {
        this.leaderId = leaderId;
    }
    /**
     * @return the prevLogIndex
     */
    public int getPrevLogIndex() {
        return prevLogIndex;
    }
    /**
     * @param prevLogIndex the prevLogIndex to set
     */
    public void setPrevLogIndex(int prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }
    /**
     * @return the prevLogTerm
     */
    public int getPrevLogTerm() {
        return prevLogTerm;
    }
    /**
     * @param prevLogTerm the prevLogTerm to set
     */
    public void setPrevLogTerm(int prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }
    /**
     * @return the entries
     */
    public LogEntries[] getEntries() {
        return entries;
    }
    /**
     * @param entries the entries to set
     */
    public void setEntries(LogEntries[] entries) {
        this.entries = entries;
    }
    /**
     * @return the leaderCommit
     */
    public int getLeaderCommit() {
        return leaderCommit;
    }
    /**
     * @param leaderCommit the leaderCommit to set
     */
    public void setLeaderCommit(int leaderCommit) {
        this.leaderCommit = leaderCommit;
    }

    @Override
    public String toString() {
        String entriesString = "";
        for (LogEntries entry : entries)
            entriesString += " i " + entry.getLogIndex() + " t " + entry.getTermNumber() + " c " + entry.getCommand();
        return "AppendEntriesArgs: term " + term +
                " leaderid " + leaderId +
                " prevLog " + prevLogIndex +
                " prevLogterm " + prevLogTerm +
                " entries " + entriesString +
                " leaderCommit " + leaderCommit;
    }
}
