/**
 * This class contains all the required information for a remote method call:
 * 1. method name
 * 2. arguments
 * 3. argument types
 * 
 * It also contains the return value and the throwables throwed by remote server
 */
package rmi;

import java.io.Serializable;
import java.util.Objects;

public class RemoteMethod implements Serializable{

    private String methodName;
    private Object[] args;
    private Class<?>[] argsType;
    Object returnValue;
    Throwable throwables;
    
    public RemoteMethod(String methodName, Object[] args, Class<?>[] argsType) {
        this.methodName = methodName;
        this.args = args;
        this.argsType = argsType;
        returnValue = null;
        throwables = null;
    }
    
    
    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }
    /**
     * @param methodName the methodName to set
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    /**
     * @return the args
     */
    public Object[] getArgs() {
        return args;
    }
    /**
     * @param args the args to set
     */
    public void setArgs(Object[] args) {
        this.args = args;
    }
    /**
     * @return the argsType
     */
    public Class<?>[] getArgsType() {
        return argsType;
    }
    /**
     * @param argsType the argsType to set
     */
    public void setArgsType(Class<?>[] argsType) {
        this.argsType = argsType;
    }
    /**
     * @return the returnValue
     */
    public Object getReturnValue() {
        return returnValue;
    }
    /**
     * @param returnValue the returnValue to set
     */
    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }
    /**
     * @return the throwables
     */
    public Throwable getThrowables() {
        return throwables;
    }
    /**
     * @param throwables the throwables to set
     */
    public void setThrowables(Throwable throwables) {
        this.throwables = throwables;
    }

}
