package uk.ac.ebi.gxa.analytics.compute;

import org.kchine.r.server.RServices;

import java.rmi.RemoteException;

/**
 * A mathematical computation task.
 *
 * @author Misha Kapushesky
 * @date Jun 19, 2009
 */
public interface ComputeTask<T> {
    public T compute(RServices R) throws RemoteException;
}
