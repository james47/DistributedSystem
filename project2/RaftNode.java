import java.io.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import lib.*;

public class RaftNode implements MessageHandling {
    private int id;
    private static TransportLib lib;
    private int num_peers;
    private volatile boolean isLeader;
    private volatile boolean isVoted;
    private volatile int votes;
    private volatile int timeout;
    private volatile int currentTerm;
    private volatile int votedFor;
    private volatile List<LogEntries> log;
    private volatile int commitIndex;
    private volatile int lastApplied;
    private volatile int[] nextIndex;
    private volatile int[] matchIndex;
    private volatile long lastReceivedTime;
    private volatile boolean[] isStale;
    private final Object logLock = new Object();
    private final Object commitLock = new Object();
    private Object[] isStaleLock;
    private Thread[] logRepThread;


    public static enum STATE {FOLLOWER, CANDIDATE, LEADER};
    private volatile STATE state;

    public RaftNode(int port, int id, int num_peers) {
        this.id = id;
        this.num_peers = num_peers;
        lib = new TransportLib(port, id, this);
        isLeader = false;
        currentTerm = 0;
        isVoted = false;
        votes = 0;
        votedFor = -1;
        commitIndex = 0;
        lastApplied = 0;
        log = new ArrayList<LogEntries>();
        lastReceivedTime = System.currentTimeMillis();
        timeout = new Random().nextInt(400) + 400;
        state = RaftNode.STATE.FOLLOWER;
        /**
         * The background thread which will kick off leader election
         * periodically by sending out RequestVote RPCs when it hasn’t
         * heard from another peer for a while
         */
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    // if election timeout, start a new election term
                    if (!isLeader && isElectionTimeout()) {

                        // if a new election is issued, reset all election-related info
                        RaftNode.this.state = RaftNode.STATE.CANDIDATE;
                        RaftNode.this.isVoted = true;
                        RaftNode.this.votes = 1;
                        RaftNode.this.votedFor = RaftNode.this.id;
                        resetElectionTimeout();

                        // increment term number
                        RaftNode.this.currentTerm++;

                        // serialize requestVoteArgs
                        int lastLogIndex = RaftNode.this.log.size();
                        int lastlogTerm = RaftNode.this.log.size() == 0 ? 0 : RaftNode.this.log.get(RaftNode.this.log.size() - 1).getTermNumber();
                        RequestVoteArgs requestVoteArgs = new RequestVoteArgs(RaftNode.this.id, RaftNode.this.currentTerm, lastlogTerm, lastLogIndex);
                        byte[] requestStream = serializeObj(requestVoteArgs);
                        BlockingQueue<Message> blockingQueue = new ArrayBlockingQueue<Message>(RaftNode.this.num_peers);


                        // send the request message to all nodes
                        // src_addr = id of this RaftNode
                        // dest_addr = all id in [0, num_peers)
                        for (int i = 0; i < RaftNode.this.num_peers; i++) {
                            if (i== RaftNode.this.id)
                                continue;
                            Message message = new Message(MessageType.RequestVoteArgs, RaftNode.this.id, i, requestStream);
                            MessageThread requestThread = new MessageThread(message, blockingQueue);
                            Thread thread = new Thread(requestThread);
                            thread.run();
                        }

                        // keep processing reply message from peers until election timeout
                        while (!isElectionTimeout()) {
                            Message replyMsg = null;
                            try {
                                long remainingTime = getElectionRemainingTime();
                                replyMsg = blockingQueue.poll(remainingTime, TimeUnit.MILLISECONDS);
                                //replyMsg = blockingQueue.take();
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            if (replyMsg != null) {
                                byte[] replyStream = replyMsg.getBody();
                                Object reply = deserializeObj(replyStream);
                                RequestVoteReply requestVoteReply = (RequestVoteReply)reply;
                                // if this candidate get vote
                                if (requestVoteReply.isVoteGranted() && RaftNode.this.state == RaftNode.STATE.CANDIDATE) {
                                    incrementVotes();
                                    // if this node get votes from majority of its peers,
                                    // it becomes the new leader
                                    if ((RaftNode.this.votes > RaftNode.this.num_peers / 2)) {
                                        RaftNode.this.isLeader = true;
                                        RaftNode.this.state = RaftNode.STATE.LEADER;
                                        RaftNode.this.votes = 0;
                                        RaftNode.this.isVoted = false;
                                        RaftNode.this.votedFor = -1;
                                        leaderInit();
                                        resetElectionTimeout();
                                        // if it becomes the leader, directly ignore all upcoming reply and
                                        // start to send AppendEntries to all followers
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    // if election hasn't time out
                    // sleep for the left time to reduce overhead
                    else {
                        try {
                            Thread.sleep(Math.max(0, getElectionRemainingTime()));
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    /*
     *call back.
     */
    @Override
    public synchronized StartReply start(int command) {
        int index;
        StartReply startReply;
        synchronized (logLock) {
            index = log.size() + 1;
            startReply = new StartReply(index, currentTerm, this.isLeader);
            if (this.isLeader) {
                log.add(new LogEntries(index, currentTerm, Integer.toString(command)));
            }
            else {
                return startReply;
            }
        }
        for (int i = 0; i < num_peers; i++) {
            if (i == id) continue;
            synchronized (isStaleLock[i]) {
                isStale[i] = true;
                isStaleLock[i].notify();
            }
        }
        return startReply;
    }

    @Override
    public GetStateReply getState() {
        GetStateReply stateReply = new GetStateReply(this.currentTerm, this.isLeader);
        return stateReply;
    }

    @Override
    public synchronized Message deliverMessage(Message message) {
        // if message is lost, return null
        if (message == null)
            return null;

        // get message type and content
        MessageType messageType = message.getType();
        byte[] requestStream = message.getBody();
        Message returnMsg = null;

        // deserialize object
        Object request = deserializeObj(requestStream);

        // if message type is RequestVoteArgs
        if (messageType.equals(MessageType.RequestVoteArgs)) {
            RequestVoteReply requestVoteReply = new RequestVoteReply(false);
            RequestVoteArgs requestVoteArgs = (RequestVoteArgs)request;
            int candidateId = requestVoteArgs.getCandidateId();
            int candidateTerm = requestVoteArgs.getCandidateTerm();
            int candidateMaxTermNumber = requestVoteArgs.getLastLogTerm();
            int candidateMaxLogIndex = requestVoteArgs.getLastLogIndex();
            System.out.println("ThisNode." + id + "receive: " + requestVoteArgs.toString());
            // if currentTerm < term, update itself to the most up-to-date term
            // if self is leader or candidate, step down to follower
            if (this.currentTerm < candidateTerm) {
                   System.out.println("ThisNode." + id + "'s term is less uo-to-date than candidate, step down to follower");
                // if self is leader (which has been obsolete)
                // step down and stop logReplicationThread
                if (this.state == RaftNode.STATE.LEADER) {
                    leaderCleanup();
                }
                this.currentTerm = candidateTerm;
                this.votes = 0;
                this.isVoted = false;
                this.votedFor = -1;
                this.isLeader = false;
                this.state = RaftNode.STATE.FOLLOWER;
            }
            // if currentTerm == term, check vote status and grant vote
            if (this.currentTerm == candidateTerm) {
                System.out.println("ThisNode." + id + "is deciding whether granting vote to the candidate...");
                if (!this.isVoted || this.votedFor == candidateId) {
                    // if the candidate is at least as up-to-date as itself, grant vote
                    int lastLogIndex = RaftNode.this.log.size();
                    int lastlogTerm = RaftNode.this.log.size() == 0 ? 0 : RaftNode.this.log.get(RaftNode.this.log.size() - 1).getTermNumber();
                    if ((candidateMaxTermNumber > lastlogTerm) || ((candidateMaxTermNumber == lastlogTerm) && (candidateMaxLogIndex >= lastLogIndex))) {
                        System.out.println("TestNode." + id + "decided to grant vote to the candidate, it's lastLogTerm is: " + lastlogTerm + ", it's lastLogIndex is: " + lastLogIndex);
                        requestVoteReply.setVoteGranted(true);
                        resetElectionTimeout();
                        this.isVoted = true;
                        this.votedFor = candidateId;
                        this.state = RaftNode.STATE.FOLLOWER;
                        this.isLeader = false;
                    }
                }
            }
            byte[] reply = serializeObj(requestVoteReply);
            returnMsg = new Message(MessageType.RequestVoteReply, this.id, candidateId, reply);
        }
        // if message is AppendEntriesArgs
        else {
            AppendEntriesArgs appendEntriesArgs = (AppendEntriesArgs)request;
            AppendEntriesReply appendEntriesReply = new AppendEntriesReply(this.currentTerm, false);
            int term = appendEntriesArgs.getTerm(); // leader's term
            int leaderId = appendEntriesArgs.getLeaderId();
            int prevLogIndex = appendEntriesArgs.getPrevLogIndex();
            int prevLogTerm = appendEntriesArgs.getPrevLogTerm();
            LogEntries[] entries = appendEntriesArgs.getEntries();
            int leaderCommit = appendEntriesArgs.getLeaderCommit();
            System.out.println("ThisNode." + id + "receive: " + appendEntriesArgs.toString());
            // reply false if term < currentTerm
            if (this.currentTerm > term) {
                System.out.println("ThisNode." + id + "reply false because it's term: " + currentTerm + " is greater than leader's term: " + term);
                // return false
                byte[] reply = serializeObj(appendEntriesReply);
                returnMsg = new Message(MessageType.AppendEntriesReply, this.id, leaderId, reply);
                return returnMsg;
            }
            // if term >= currentTerm, set currentTerm = term, convert to follower
            else {
                System.out.println("ThisNode." + id + "'s term is not greater than leader, covert to follower");
                if (this.isLeader) {
                    leaderCleanup();
                }
                this.state = RaftNode.STATE.FOLLOWER;
                this.isLeader = false;
                resetElectionTimeout();
                setCurrentTerm(term);
                this.votes = 0;
                this.isVoted = false;
                this.votedFor = -1;
            }
            // reply false if log doesn't contain an entry at prevLogIndex whose term matches prevLogTerm
            if (prevLogIndex != 0 && ((this.log.size() < prevLogIndex) || (this.log.get(prevLogIndex - 1).getTermNumber() != prevLogTerm))) {
                System.out.println("ThisNode." + id + "reply false because it does not cantain an entry at prevLogIndex whose term matches prevLogTerm");
                byte[] reply = serializeObj(appendEntriesReply);
                returnMsg = new Message(MessageType.AppendEntriesReply, this.id, leaderId, reply);
                return returnMsg;
            }
            System.out.println("ThisNode." + id + "start to synchronize its log with leader's log");
            System.out.printf("Before syn, log is : ");
            for (int i = 0; i < log.size(); i++) 
                System.out.print(log.get(i).toString());
            System.out.println("");
            synchronized (logLock) {
                // update entries after prevLogIndex by
                // 1. remove all entries after prevLogIndex and
                int len = this.log.size();
                for (int i = len - 1; i >= prevLogIndex; i--)
                    this.log.remove(i);
                // 2. append replace them with new entries
                for (int i = 0; i < entries.length; i++)
                    this.log.add(entries[i]);
            }
            System.out.printf("After syn, log is : ");
            for (int i = 0; i < log.size(); i++) 
                System.out.print(log.get(i).toString());
            System.out.println("");
            // update commitIndex
            if (leaderCommit > this.commitIndex) {
                System.out.println("ThisNode." + id + "find out it need to update commitIndex...");
                commitIndex = Math.min(leaderCommit, this.log.size());
                applyLog();
            }
            appendEntriesReply.setSuccess(true);
            appendEntriesReply.setTerm(this.currentTerm); // TODO: use old term or updated term?
            byte[] reply = serializeObj(appendEntriesReply);
            returnMsg = new Message(MessageType.AppendEntriesReply, this.id, leaderId, reply);
            return returnMsg;
        }
        return returnMsg;
    }

    public boolean isElectionTimeout() {
        if ((System.currentTimeMillis() - this.lastReceivedTime) > this.timeout)
            return true;
        return false;
    }


    /*
     * get the remaining time of the next election timeout
     */
    public long getElectionRemainingTime() {
        return this.timeout - (System.currentTimeMillis() - this.lastReceivedTime);
    }


    public void resetElectionTimeout() {
        this.lastReceivedTime = System.currentTimeMillis();
    }


    public void setCurrentTerm(int term) {
        this.currentTerm = term;
    }


    /**
     * Serialize Object to byte[]
     * @param obj
     * @return
     */
    public byte[] serializeObj(Object obj) {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput output = null;
        byte[] bytesObj = null;
        try {
            output = new ObjectOutputStream(bos);
            output.writeObject(obj);
            output.flush();
            bytesObj = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bytesObj;
    }


    /**
     * Deserialize byte[] to Object
     * @param stream
     * @return
     */
    public Object deserializeObj(byte[] stream) {
        Object obj = new Object();
        ByteArrayInputStream bis = new ByteArrayInputStream(stream);
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(bis);
            obj = in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return obj;
    }


    public void incrementVotes() {
        this.votes++;
    }


    /*
     * if commitIndex > lastApplied, increment lastApplied, apply log[lastApplied]
     * to state machine, loop until commitIndex == lastApplied
     */
    private void applyLog() {
        while (commitIndex > lastApplied) {
            System.out.println("ThisNode." + id + "commitIndex-" + commitIndex + "is greater than lastApplied-" + lastApplied + ", applyChannel");
            lastApplied ++;
            ApplyMsg applyMsg = new ApplyMsg(id,
                    lastApplied,
                    Integer.parseInt(log.get(lastApplied - 1).getCommand()),
                    false,
                    null);
            try {
                lib.applyChannel(applyMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    /*
     * open a new thread to send the message and
     * fetch the reply message and put it into blocking queue
     */
    private class MessageThread implements Runnable {
        Message message;
        BlockingQueue<Message> blockingQueue;
        public MessageThread(Message message, BlockingQueue<Message> blockingQueue) {
            this.message = message;
            this.blockingQueue = blockingQueue;
            // TODO Auto-generated constructor stub
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                Message message = lib.sendMessage(this.message);
                if (message != null && blockingQueue != null)
                    blockingQueue.offer(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    private void leaderInit() {
        isStaleLock = new Object[num_peers];
        isStale = new boolean[num_peers];
        matchIndex = new int[num_peers];
        nextIndex = new int[num_peers];
        logRepThread = new Thread[num_peers];
        for (int i = 0; i < num_peers; i++) {
            isStale[i] = true;
            matchIndex[i] = 0;
            nextIndex[i] = log.size() + 1;
            isStaleLock[i] = new Object();
        }
        state = STATE.LEADER;
        isLeader = true;
        for (int i = 0; i < num_peers; i++) {
            if (i == id) continue;
            LogReplicateThread thread = new LogReplicateThread(id, i);
            logRepThread[i] = new Thread(thread);
            logRepThread[i].start();
        }
    }

    private void leaderCleanup() {
        isLeader = false;
        state = STATE.FOLLOWER;
        for (int i = 0; i < num_peers; i++) {
            if (i == id) continue;
            logRepThread[i].interrupt();
        }
    }

    private class LogReplicateThread implements Runnable {
        int leader_id, peer_id;
        public LogReplicateThread(int leader_id, int peer_id) {
            this.leader_id = leader_id;
            this.peer_id = peer_id;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (isStaleLock[peer_id]) {
                    if (!isStale[peer_id])
                        try {
                            isStaleLock[peer_id].wait(120);
                        } catch (InterruptedException e) {
                            return;
                        }
                }

                // sendMessage preparation
                int prevLogIndex = nextIndex[peer_id] - 1;
                int lastLogIndex = log.size();
                int lastCommitIndex = commitIndex;
                if (lastLogIndex < prevLogIndex)
                    prevLogIndex = lastLogIndex;
                LogEntries[] entries = new LogEntries[lastLogIndex - prevLogIndex];
                for (int i = 0; i < entries.length; i++) {
                    entries[i] = log.get(prevLogIndex - 1 + i + 1);
                }
                AppendEntriesArgs appendEntriesArgs = new AppendEntriesArgs(currentTerm,
                        leader_id,
                        prevLogIndex,
                        prevLogIndex == 0? 0: log.get(prevLogIndex - 1).getTermNumber(),
                        entries,
                        lastCommitIndex);
                byte[] args = serializeObj(appendEntriesArgs);

                // sendMessage
                Message reply = null;
                try {
                    reply = lib.sendMessage(new Message(MessageType.AppendEntriesArgs, leader_id, peer_id, args));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                // process reply
                if (!Thread.currentThread().isInterrupted()) {
                    // reply == null, timeout
                    if (reply == null) {
                        continue;
                    }
                    AppendEntriesReply appendEntriesReply = (AppendEntriesReply) deserializeObj(reply.getBody());

                    // step down if term is out-dated
                    if (appendEntriesReply.getTerm() > currentTerm) {
                        leaderCleanup();
                        return;
                    }

                    // if success
                    if (appendEntriesReply.isSuccess()) {
                        // update nextIndex, matchIndex
                        nextIndex[peer_id] = lastLogIndex + 1;
                        matchIndex[peer_id] = lastLogIndex;

                        // try to update commitIndex
                        for (int N = matchIndex[peer_id]; N > commitIndex; N--) {
                            if (log.get(N - 1).getTermNumber() != currentTerm)
                                continue;
                            int count = 0;
                            for (int i = 0; i < num_peers; i++)
                                if (i == leader_id || matchIndex[i] >= N)
                                    count ++;
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }

                            if (count > num_peers / 2) {
                                synchronized (commitLock) {
                                    if (commitIndex < N) {
                                        commitIndex = N;
                                        applyLog();
                                        for (int i = 0; i < num_peers; i++) {
                                            if (i == leader_id) continue;
                                            synchronized (isStaleLock[i]) {
                                                isStale[i] = true;
                                                isStaleLock[i].notify();
                                            }
                                        }
                                    }
                                }
                                // satisfied N found
                                break;
                            }
                        }
                        // see if up-to-date log is replicated on peer_id
                        synchronized (isStaleLock[peer_id]) {
                            if (lastLogIndex == log.size() && lastCommitIndex == commitIndex) {
                                isStale[peer_id] = false;
                            }
                        }
                    }
                    // failed, decrease nextIndex and retry
                    else {
                        nextIndex[peer_id] --;
                    }
                }
            }
        }
    }

    //main function
    public static void main(String args[]) throws Exception {
        if (args.length != 3) throw new Exception("Need 2 args: <port> <id> <num_peers>");
        //new usernode
        RaftNode UN = new RaftNode(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
    }
}
