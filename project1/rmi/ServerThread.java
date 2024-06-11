package rmi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

public class ServerThread<T> implements Runnable {

    private Skeleton<T> skeleton;

    public ServerThread(Skeleton<T> skeleton) {
        this.skeleton = skeleton;
    }

    public void run() {
        ServerSocket listenSocket = skeleton.getServerSocket();
        Socket clientSocket;
        List<Thread> workers = new ArrayList<Thread>();

        try {
            while (true) {
                // block on accept()
                clientSocket = listenSocket.accept();
                // create new thread for method invoking request
                Thread workerThread = new Thread(new WorkerThread<T>(skeleton, clientSocket));
                workers.add(workerThread);
                workerThread.start();
            }
        } catch (IOException e) {
            // join all workers before exit to make sure stopped() is called afterwards
            // to improve, should kill worker if it runs forever(endless loop)
            for (Thread worker : workers)
                try {
                    worker.join();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    skeleton.listen_error(ie);
                }
            return;
        }
    }
}
