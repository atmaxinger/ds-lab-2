package nameserver;

import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

import java.io.Serializable;
import java.rmi.RemoteException;


public class NameserverForChatserverImpl implements INameserverForChatserver, Serializable{
    @Override
    public void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

    }

    @Override
    public INameserverForChatserver getNameserver(String zone) throws RemoteException {
        return null;
    }

    @Override
    public String lookup(String username) throws RemoteException {
        return null;
    }
}
