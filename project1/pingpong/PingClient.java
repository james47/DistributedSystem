/**
 * A PingClient which creates a stub, forwards all method calls
 * to a server by contacting a remote skeleton.
 * 
 * It will perform a simple test by calling the method ping()
 * for 4 times and calculate the number of failure
 */

package pingpong;

import rmi.RMIException;

public class PingClient {
    private static final int TEST_TIME = 4;

    /**
     * @param args
     */
    public static void main(String[] args) {
        int cnt = 0;
        pingpongInterface pingServer = null;
        try {
            pingServer = PingServerFactory.makePingServer(PingServer.HOSTNAME, PingServer.PORT);
        } catch (Exception e) {
           e.printStackTrace();
        }
        
        // test the remote method call for TEST_TIME times
        // and print the results
        for (int i = 0; i < TEST_TIME; i++) {
            try {
                String returnStr = pingServer.ping(i);
                if (!returnStr.equals("Pong" + i))
                    cnt++;
                
            } catch (RMIException e) {
                cnt++;
                e.printStackTrace();
            }
        }
        printResult(TEST_TIME, cnt);
    }
    

    public static void printResult(int total, int cnt) {
        System.out.println(TEST_TIME + " Test completed, " + cnt + " Tests Failed");
    }
    
}
