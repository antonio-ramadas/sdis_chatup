package chatup.room;

import chatup.user.UserMessage;
import javafx.util.Pair;

import java.util.*;

public class Room {
	
	private String roomName;
	private String roomPassword;
	private String roomOwner;

	private TreeSet<UserMessage> roomMessages;
	private HashMap<String, String> roomUsers;
	private Set<Integer> roomServers;

	public Room(final String paramName, final String paramPassword,final String paramOwner) {
		roomName = paramName;
		roomOwner = paramOwner;
		roomPassword = paramPassword;
		roomMessages = new TreeSet<>();
		roomUsers = new HashMap<>();
		roomServers = new HashSet<>();
	}

	public Room(final String roomName, final String roomOwner) {
		this(roomName, null, roomOwner);
	}

	public void registerMessage(final String userToken, int roomId, final String messageBody) {
		roomMessages.add(new UserMessage(messageBody, userToken, roomId, (new Date()).getTime()));
	}

	public UserMessage[] getMessages() {
		return (UserMessage[]) roomMessages.toArray();
	}

	public boolean registerServer(int serverId) {

		if (roomServers.contains(serverId)) {
			return false;
		}

		roomServers.add(serverId);

		return true;
	}

	public boolean removeServer(int serverId) {

		if (!roomServers.contains(serverId)) {
			return false;
		}

		roomServers.remove(serverId);

		return true;
	}

	public boolean registerUser(final Pair<String, String> userAccount) {

		final String userToken = userAccount.getKey();

		if (roomUsers.containsKey(userToken)) {
			return false;
		}

		roomUsers.put(userToken, userAccount.getValue());

		return true;
	}

	public boolean removeUser(final Pair<String, String> userAccount)  {

		final String userToken = userAccount.getKey();

		if (!roomUsers.containsKey(userToken)) {
			return false;
		}

		roomUsers.remove(userToken);

		return true;
	}

	public boolean isEmpty() {
		return roomUsers.size() == 0;
	}

	public boolean isPrivate() {
		return roomPassword != null;
	}

	public final String getName() {
		return roomName;
	}

	public final String getOwner() {
		return roomOwner;
	}

	public final String getPassword() {
		return roomPassword;
	}

	public final HashMap<String, String> getUsers() {
		return roomUsers;
	}

	public boolean hasUser(final String userToken) {
		return roomUsers.containsKey(userToken);
	}
}