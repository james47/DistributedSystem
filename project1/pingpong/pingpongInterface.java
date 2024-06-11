/**
 * Interface of PingPong System includes only one method:
 * ping()
 */
package pingpong;

import rmi.RMIException;

public interface pingpongInterface {
    public String ping(int idNumber) throws RMIException;
}
