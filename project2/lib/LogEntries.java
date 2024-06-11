/**
 * 
 */
package lib;

import java.io.Serializable;

/**
 * LogEntry - This class defines the components of a Raft log entry:
 * logIndex : unique incremental index of this log entry
 * termNumber : term number
 * command : the command of the log entry which can be applied to 
 * the state machine, same content and order of log entry will result
 * in the same execution result in state machine
 */
public class LogEntries implements Serializable {

    private static final long serialVersionUID = 1L;
    private int logIndex;
    private int termNumber;
    private String command;
    
    public LogEntries(int logIndex, int termNumber, String command) {
        this.logIndex = logIndex;
        this.termNumber = termNumber;
        this.command = command;
    }

    /**
     * @return the logIndex
     */
    public int getLogIndex() {
        return logIndex;
    }

    /**
     * @param logIndex the logIndex to set
     */
    public void setLogIndex(int logIndex) {
        this.logIndex = logIndex;
    }

    /**
     * @return the termNumber
     */
    public int getTermNumber() {
        return termNumber;
    }

    /**
     * @param termNumber the termNumber to set
     */
    public void setTermNumber(int termNumber) {
        this.termNumber = termNumber;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }


}
