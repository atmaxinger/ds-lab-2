package nameserver;

import cli.Shell;
import entity.User;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class NameserverRemote implements INameserver, Serializable {

    private Shell shell;
    private HashMap<String, INameserver> nameserverHashMap;
    private List<User> userList;

    private final String WRONG_USER_OR_NOT_REGISTERED = "Wrong username or user not registered.";


    public NameserverRemote(Shell shell) {
        this.shell = shell;
        nameserverHashMap = new HashMap<>();
        userList = new ArrayList<>();
    }

    @Override
    public void registerNameserver(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

        String domainParts[] = domain.split("\\.");

        if (domainParts.length == 1) {

            synchronized (nameserverHashMap) {

                if (nameserverHashMap.containsKey(domain)) {
                    throw new AlreadyRegisteredException(String.format("'%s' has already been registered", domain));
                }

                nameserverHashMap.put(domain, nameserver);
            }

            try {
                shell.writeLine(String.format("Registering nameserver for zone ’%s’%n", domain));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (domainParts.length > 1) {

            INameserver topNameserver = getTopNameserver(domainParts);

            String cutDomain = cutTopDomain(domainParts);

            topNameserver.registerNameserver(cutDomain, nameserver, nameserverForChatserver);

        }
    }

    @Override
    public void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

        String usernameParts[] = username.split("\\.");

        if (usernameParts.length == 1) {

            User user = new User();
            user.setUsername(username);

            String addressParts[] = address.split(":");
            user.setIp(addressParts[0]);
            user.setPort(Integer.parseInt(addressParts[1]));

            userList.add(user);

            try {
                shell.writeLine(String.format("Registering user ’%s’%n", username));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (usernameParts.length > 1) {
            INameserver topNameserver = getTopNameserver(usernameParts);

            String cutUsername = cutTopDomain(usernameParts);

            topNameserver.registerUser(cutUsername, address);
        }
    }

    @Override
    public INameserverForChatserver getNameserver(String zone) throws RemoteException {
        try {
            shell.writeLine(String.format("Nameserver for ’%s’ requested by chatserver%n", zone));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getNameserverFromHashMap(zone);

    }

    @Override
    public String lookup(String username) throws RemoteException {

        try {
            shell.writeLine(String.format("Address for user ’%s’ requested by chatserver%n", username));
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (User user : userList) {
            if (user.getUsername().equals(username)) {
                return String.format("%s:%s", user.getIp(), user.getPort());
            }
        }

        return WRONG_USER_OR_NOT_REGISTERED;
    }

    private String cutTopDomain(String[] domainParts) {
        String subdomain = "";

        for (int i = 0; i < domainParts.length - 1; i++) {
            subdomain += domainParts[i];

            if (i + 1 != domainParts.length - 1) {
                subdomain += ".";
            }
        }
        return subdomain;
    }

    private INameserver getTopNameserver(String[] domainParts) throws InvalidDomainException {

        String parentDomain = domainParts[domainParts.length - 1];

        INameserver parentNameserver = getNameserverFromHashMap(parentDomain);

        if (parentNameserver == null) {
            throw new InvalidDomainException(String.format("no nameserver for %s", parentNameserver));
        }
        return parentNameserver;
    }

    public Set<String> getDomains() {
        synchronized (nameserverHashMap) {
            return nameserverHashMap.keySet();
        }
    }

    public List<User> getUserList() {
        return userList;
    }

    private INameserver getNameserverFromHashMap(String key)
    {
        synchronized (nameserverHashMap)
        {
            return nameserverHashMap.get(key);
        }
    }
}
