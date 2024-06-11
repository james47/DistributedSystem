import lib.Block;
import lib.GetStateReply;

import java.util.ArrayList;
import java.util.List;

public class BlockchainTest {

    private static final int NUM_ARGS = 2;
    private static int controllerPort = 0;

    /*  The tester generously allows solutions to complete elections in one second
     *  (much more than the paper's range of timeouts).
     */
    private static int BROADCAST_TIME = 1000;

    private static void TestInitialState() throws Exception {

        int numServers = 3;

        Config cfg = new Config( numServers, controllerPort );

        System.out.println( "Testing initial state ..." );
        System.out.println( "Initiating nodes ..." );

        /* Waiting for all raft peers to start and register with Transport Layer controller. */
        cfg.waitUntilAllRegister();


        int max = 0;

        System.out.println( "Checking initial state ..." );

        for(int i = 0; i < numServers; i++) {
            GetStateReply rep = cfg.getState(i);

            int l = rep.length;
            if(max < l)
                max = l;
        }

        if( max != 1 ) {
            System.err.println( "Warning : Initial state error, length should be 1. \n" );
        } else {
            System.out.println( " .... Passed ! " );
        }

        cfg.cleanup();

        System.out.println("####### Test done ##########");
    }

    private static void TestBasicMine() throws Exception {

        int numServers = 3;

        Config cfg = new Config( numServers, controllerPort );

        /* Waiting for all raft peers to start and register with Transport Layer controller. */
        cfg.waitUntilAllRegister();

        Thread.sleep( BROADCAST_TIME );

        System.out.println( "Testing single-node mining request ...\\n" );

        /* Send a mining request to node 0. */
        String s = "Node0 mine";
        Block b = cfg.sendMiningRequestToNode(0, s.getBytes());

        /* check the length. */
        GetStateReply reply = cfg.getState(0);

        if(reply.length >= 2) {
            System.out.println( "Warning : Should not add the block into the " +
                    "chain before broadcast to the network. \n" );

        }

        cfg.broadcastNewBlock(0);

        Thread.sleep( BROADCAST_TIME );
        boolean pass = true;

        for(int i = 0; i < numServers; i++) {
            GetStateReply rep = cfg.getState(i);

            int l = rep.length;
            if(l != 2) {
                pass = false;
                System.out.println( "Error : Broadcast error! Node " + i +
                        " length should be 2. \n" );
            }

            if(!rep.lastHash.startsWith("00000")) {
                pass = false;
                System.out.println( "Error : The hash is not not difficulty 20!" );
            }
        }

        if(pass)
            System.out.println( "  ... Passed\n" );

        cfg.cleanup();

        System.out.println("####### Test done ##########");
    }

    private static void TestBasicMultiMining() throws Exception {

        int numServers = 3;

        Config cfg = new Config( numServers, controllerPort );

        /* Waiting for all raft peers to start and register with Transport Layer controller. */
        cfg.waitUntilAllRegister();

        Thread.sleep( BROADCAST_TIME );

        System.out.println( "Testing one mining request ...\\n" );

        /* Send a mining request to each node. */
        int cnt = 6;
        for(int node = 0; node < cnt; node ++) {
            int id = node % numServers;
            String s = "Test node " + id + " mine";
            Block b = cfg.sendMiningRequestToNode(id, s.getBytes());

            /* check the length. */
            GetStateReply reply = cfg.getState(id);

            cfg.broadcastNewBlock(id);

            Thread.sleep(BROADCAST_TIME);
        }
        boolean pass = true;

        for(int i = 0; i < numServers; i++) {
            GetStateReply rep = cfg.getState(i);

            int l = rep.length;
            if(l != cnt + 1) {
                pass = false;
                System.out.println( "Error : Broadcast error! Node " + i +
                        " length should be 2. \n" );
            }
        }

        if(pass)
            System.out.println( "  ... Passed\n" );

        cfg.cleanup();

        System.out.println("####### Test done ##########");
    }

    private static void TestConsensus() throws Exception {

        int numServers = 4;

        Config cfg = new Config( numServers, controllerPort );

        System.out.println( "Testing consensus within multiple nodes.\n" );

        System.out.println( "Initiating nodes ..." );

        /* Waiting for all raft peers to start and register with Transport Layer controller. */
        cfg.waitUntilAllRegister();

        Thread.sleep(BROADCAST_TIME);

        System.out.println( "Launch concurrent mining requests.\n" );

        List<Thread> threads = new ArrayList<>();

        int cnt = 4;
        for( int i = 0; i < cnt; i++ ) {
            int nodeID = i;

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    int id = nodeID;
                    String s = "Thread node " + id + " mine";
                    Block b = cfg.sendMiningRequestToNode(id, s.getBytes());

                    cfg.broadcastNewBlock(id);
                }
            });
            threads.add(t);
            t.start();
        }

        for(Thread t : threads){
            t.join();
        }

        Thread.sleep(BROADCAST_TIME);
        boolean pass = true;

        String lastHash = null;

        System.out.println( "Check final chain length and hash..." );

        for(int i = 0; i < numServers; i++) {
            GetStateReply rep = cfg.getState(i);

            int l = rep.length;
            if(l != 2) {
                pass = false;
                System.out.println( "Error : Broadcast error! Node " + i +
                        " length should be " + 2 +". \n" );
            }

            if(lastHash == null){
                lastHash = rep.lastHash;
            }
            else {
                if(lastHash.compareToIgnoreCase(rep.lastHash) != 0) {
                    pass = false;
                    System.out.println("Error : Block not consensus! Prev: " +
                            lastHash + ", cur: " + rep.lastHash);
                }
            }
        }

        if(pass)
            System.out.println( "  ... Passed\n" );

        cfg.cleanup();

        System.out.println("####### Test done ##########");
    }

    private static void TestReconnectConsensus() throws Exception {
        int numServers = 4;

        Config cfg = new Config( numServers, controllerPort );

        System.out.println( "Testing consensus after disconnection.\n" );

        System.out.println( "Initiating nodes ..." );

        /* Waiting for all raft peers to start and register with Transport Layer controller. */
        cfg.waitUntilAllRegister();

        Thread.sleep(BROADCAST_TIME);

        cfg.disconnect(3);

        Thread.sleep(BROADCAST_TIME);
        int cnt = 2, id = 0;
        for(int i = 0; i < cnt; i++) {
            String s = "Test node " + id + " mine";
            Block b = cfg.sendMiningRequestToNode(id, s.getBytes());

            cfg.broadcastNewBlock(id);

            Thread.sleep(BROADCAST_TIME);
        }

        Thread.sleep(BROADCAST_TIME);

        String lastHash = null;

        System.out.println( "Check length and hash..." );
        boolean pass = true;

        for(int i = 0; i < numServers - 1; i++) {
            GetStateReply rep = cfg.getState(i);

            int l = rep.length;
            if(l != cnt + 1) {
                pass = false;
                System.out.println( "Error : Broadcast error! Node " + i +
                        " length should be " + (cnt + 1) +". \n" );
            }

            if(lastHash == null){
                lastHash = rep.lastHash;
            }
            else {
                if(lastHash.compareToIgnoreCase(rep.lastHash) != 0) {
                    pass = false;
                    System.out.println("Error : Block not consensus! Prev: " +
                            lastHash + ", cur: " + rep.lastHash);
                }
            }
        }

        System.out.println( "Reconnect server 3..." );

        cfg.connect(3);

        cfg.downloadChain(3);

        Thread.sleep(BROADCAST_TIME);

        System.out.println( "Check final status..." );

        GetStateReply rep = cfg.getState(3);

        if(rep.length != cnt + 1){
            pass = false;
            System.out.println( "Error: Final length wrong:" + rep.length + ", should be " + (cnt + 1) );
        }

        if(lastHash.compareToIgnoreCase(rep.lastHash) != 0) {
            pass = false;
            System.out.println("Error Final hash wrong: " + rep.lastHash  + ", should be " + lastHash);
        }

        if(pass)
            System.out.println( "  ... Passed\n" );

        cfg.cleanup();

        System.out.println("####### Test done ##########");
    }


    public static void main( String[] args ) throws InterruptedException {

        if( args.length != NUM_ARGS ) {
            System.err.println( "Invalid number of arguments\n" );
            return;
        }

        String testCase = args[0];
        controllerPort = Integer.parseInt( args[1] );

        try {
            switch (testCase) {
                case "Initial-State":
                    TestInitialState();
                    break;

                case "Basic-Mining":
                    TestBasicMine();
                    break;

                case "Basic-MultiMining":
                    TestBasicMultiMining();
                    break;

                case "Consensus":
                    TestConsensus();
                    break;

                case "Reconnect-Consensus":
                    TestReconnectConsensus();
                    break;
            }
        } catch (Exception e) {
                e.printStackTrace();
        }
    }
}