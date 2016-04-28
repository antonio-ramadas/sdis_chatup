package chatup.server;

import java.util.HashMap;

public class SecondaryServer extends Server
{
	private HashMap<String, UserSession> users;
	private HashMap<Integer, ServerInfo> servers;
}