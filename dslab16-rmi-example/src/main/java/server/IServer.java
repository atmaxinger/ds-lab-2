package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Declare a common interface which is understood by both the client and the
 * server, for accessing the remote object in an uniform manner.
 */
public interface IServer extends Remote {

	public String ping(String clientName) throws RemoteException;

}
