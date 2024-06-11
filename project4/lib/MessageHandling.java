package lib;
/**
 * This Interface is to define the basic API for a raft node.
 *
 * @author Sphoorti Joglekar, Priya Avhad, Yijia Cui, Zonglin Wang
 */

public interface MessageHandling {
   /**
    * This API is the callback for the network to deliver a message to a raft
    * node, all the message packet will eventually be delivered to target node
    * from the sender if you call sendMessage() method of TransportLib.
    *
    * @param message the message this node receives.
    * @return the respond message packet.
    */
   Message deliverMessage(Message message);
   /**
    * This API is to return the current node status required in GetStateReply
    * class, the testing framework may call this function at any time to check
    * this node's status. Read the document of GetStateReply class for all the
    * information that you need to provide.
    *
    * @return the node status packet.
    */
   GetStateReply getState();


   byte[] mineNewBlock(byte[] data);

   void broadcastNewBlock();

   void downloadChain();
}
