package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

public class WorkerThread<T> implements Runnable {

    private T server;
    private Socket socket;
    private Skeleton<T> skeleton;

    public WorkerThread(Skeleton<T> skeleton, Socket socket) {
        this.server = skeleton.getServer();
        this.socket = socket;
        this.skeleton = skeleton;
    }

    public void run() {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        // create object iostream
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            skeleton.service_error(new RMIException("failed to create iostream"));
        }

        // the protocol between stub and skeleton is just an Object
        // read the request object, invoke desired method and return result / exception
        RemoteMethod remoteMethod = null;
        try {

            remoteMethod = (RemoteMethod)in.readObject();
            Method method = server.getClass().getMethod(remoteMethod.getMethodName(), remoteMethod.getArgsType());
            remoteMethod.setReturnValue( method.invoke(server, remoteMethod.getArgs()) );
            out.writeObject(remoteMethod);

        } catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {

            e.printStackTrace();
            skeleton.service_error(new RMIException(e));
            remoteMethod.setThrowables(e);
            try {
                out.writeObject(remoteMethod);
            } catch (IOException e1) {
                e1.printStackTrace();
                skeleton.service_error(new RMIException(e1));
            }

        } catch (InvocationTargetException e) {

            e.printStackTrace();
            remoteMethod.setThrowables(e.getTargetException());
            try {
                out.writeObject(remoteMethod);
            } catch (IOException e1) {
                e1.printStackTrace();
                skeleton.service_error(new RMIException(e1));
            }
        }

        // close stream and socket
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            skeleton.service_error(new RMIException("failed to close"));
        }
    }

}
