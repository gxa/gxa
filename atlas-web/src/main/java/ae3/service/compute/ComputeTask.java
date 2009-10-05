package ae3.service.compute;

import org.kchine.r.server.RServices;

import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop
 * Date: Jun 19, 2009
 * Time: 3:00:19 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ComputeTask<T> {
    public T compute(RServices R) throws RemoteException;
}
