/**
 * A dynamic proxy which implements interfaces specified at runtime such
 * that a method invocation can be encoded and dispatched to the remote
 * server, and get the return result from the server.
 * 
 * It overrides three methods:
 * 1. equals
 * 2. hashCode
 * 3. toString
 */
package rmi;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class DynamicProxy<T> implements InvocationHandler {

    private InetSocketAddress socketAddress;
    private Class<T> c;

    public DynamicProxy(InetSocketAddress address, Class<T> c) {
        this.socketAddress = address;
        this.c = c;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (method == null)
            return new NullPointerException();    
        
        // override three methods
        if (method.getName().equals("equals")) {
            if (args.length != 1)
                throw new IllegalArgumentException();
            else
                return stubEquals(proxy, args[0]);
        }

        if (method.getName().equals("hashCode")) {
                return stubHashCode(proxy);
        }

        if (method.getName().equals("toString")) {
                return stubToString(proxy);
        }
        
        // get return result from remote method invocation
        Object returnObj = remoteMethodInvocation(method, args);
        return returnObj;
    }

    /*
     * Remote function call
     */
    private Object remoteMethodInvocation(Method method, Object[] args) throws Exception {

        InetSocketAddress socketAddress = this.getSocketAddress();
        InetAddress address = socketAddress.getAddress();
        int port = socketAddress.getPort();
        Socket socket = null;
        ObjectOutputStream objOutputStream = null;
        ObjectInputStream objInputStream = null;
        RemoteMethod remoteReturnValue = null;
        Object returnValue = null;
        try {
            socket = new Socket(address, port);

            // serialize the method and its arguments and write to output stream
            objOutputStream = new ObjectOutputStream(socket.getOutputStream());
            RemoteMethod remoteMethod = new RemoteMethod(method.getName(), args, method.getParameterTypes());
            objOutputStream.writeObject(remoteMethod);
            objOutputStream.flush();
            
            // get and deserialize return object
            objInputStream = new ObjectInputStream(socket.getInputStream());
            remoteReturnValue = (RemoteMethod) objInputStream.readObject();
            
            // if remote method returns throwables, return the throwables
            // else return the result of remote method call
        } catch (Exception e) {
            e.printStackTrace();
            closeResource(socket, objInputStream, objOutputStream);
            // TODO: Error msg
            // throw new RMIException("RMI Error");
            throw new RMIException("RMI Error", e);
        }

        if (remoteReturnValue.getThrowables() != null) {
            returnValue = remoteReturnValue.getThrowables();
            throw (Exception) returnValue;
        }
        else
            returnValue = remoteReturnValue.getReturnValue();
        closeResource(socket, objInputStream, objOutputStream);
        return returnValue;
    }
    
    /*
     * close all the opened resources and 
     * throw corresponding exceptions if there's any
     */
    private void closeResource(Socket socket, ObjectInputStream objInputStream, ObjectOutputStream objOutputStream) {
        try {
            if (socket != null)
                socket.close();
            if (objInputStream != null)
                objInputStream.close();
            if (objOutputStream != null)
                objOutputStream.close();
        } catch (Exception e) {
           e.printStackTrace();
        }
    }

    /*
     * equals method between two stubs two stubs are considered equal if: 1. they
     * implement the same remote interface 2. if they connect to the same
     * skeleton(has the same host name and port number)
     */
    public boolean stubEquals(Object proxy1, Object proxy2) {
        
        // null equals null
        if (proxy1 == null && proxy2 == null)
            return true;
        
        // if one proxy is null and the other one is not,
        // these two are considered not equal
        if (proxy1 == null || proxy2 == null)
            return false;
        
        // if neither of two proxies is null, compare based on the remote interfaces they 
        // implemented and the skeleton they connected to
        DynamicProxy<T> invoHandler1 = (DynamicProxy<T>) Proxy.getInvocationHandler(proxy1);
        DynamicProxy<T> invoHandler2 = (DynamicProxy<T>) Proxy.getInvocationHandler(proxy2);
        InetSocketAddress socketAddress1 = invoHandler1.getSocketAddress();
        InetSocketAddress socketAddress2 = invoHandler2.getSocketAddress();
        Class<T> class1 = invoHandler1.getC();
        Class<T> class2 = invoHandler2.getC();
        if (class1.getName().equals(class2.getName())
                && socketAddress1.getHostName().equals(socketAddress2.getHostName())
                && socketAddress1.getPort() == socketAddress2.getPort()) {
            return true;
        }
        return false;
    }

    /*
     * calculate the proxy's hashcode based on the value
     * of its toString value
     */
    public int stubHashCode(Object proxy) {
        // assign minimum value of integer to null
        if (proxy == null)
            return Integer.MIN_VALUE;
        String proxyStr = stubToString(proxy);
        return proxyStr.hashCode();
    }
    
    /*
     * toString will return the information about the proxy's
     * implemented remote interface and the remote address
     * of the skeleton it connects to
     */
    public String stubToString(Object proxy) {
        DynamicProxy<T> invoHandler = (DynamicProxy<T>) Proxy.getInvocationHandler(proxy);
        InetSocketAddress socketAddress = invoHandler.getSocketAddress();
        Class<T> c = invoHandler.getC();
        StringBuilder sb = new StringBuilder();
        sb.append("Remote interface implemented by this stub: ");
        sb.append(c.getName());
        sb.append("\nRemote address of the skeleton it connects to: ");
        sb.append(socketAddress.getHostName() + ":" + socketAddress.getPort());
        return sb.toString();
    }

    public Class<T> getC() {
        return c;
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }
}
