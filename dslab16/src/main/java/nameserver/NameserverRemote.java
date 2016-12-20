package nameserver;

import cli.Shell;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Set;

public class NameserverRemote implements INameserver, Serializable{

private Shell shell;
    private HashMap<String,INameserver> nameserverHashMap;
    private HashMap<String,INameserverForChatserver> nameserverForChatserverHashMap;


    public NameserverRemote(Shell shell) {
this.shell = shell;
        nameserverHashMap = new HashMap<>();
        nameserverForChatserverHashMap = new HashMap<>();
    }

    @Override
    public void registerNameserver(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

        String domainParts[] = domain.split("\\.");

        if(domainParts.length == 1)
        {

            if(nameserverHashMap.containsKey(domain)){
                throw new AlreadyRegisteredException(String.format("'%s' has already been registered",domain));
            }

            nameserverHashMap.put(domain,nameserver);
            nameserverForChatserverHashMap.put(domain,nameserverForChatserver);

            try {
                shell.writeLine(String.format("Registering nameserver for zone ’%s’%n", domain));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(domainParts.length > 1)
        {
            String parentDomain = domainParts[domainParts.length-1];

            INameserver parentNameserver = nameserverHashMap.get(parentDomain);

            if(parentNameserver == null)
            {
                throw new InvalidDomainException(String.format("no nameserver for %s",parentNameserver));
            }

            String subdomain = "";

            for(int i = 0; i < domainParts.length-1; i++)
            {
                subdomain += domainParts[i];

                if(i+1 != domainParts.length-1)
                {
                    subdomain += ".";
                }
            }

            parentNameserver.registerNameserver(subdomain,nameserver,nameserverForChatserver);

        }
    }

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

    public Set<String> getDomains()
    {
        return nameserverHashMap.keySet();
    }
}
