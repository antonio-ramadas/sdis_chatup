package chatup.room;

import chatup.user.MessageCache;
import chatup.user.UserMessage;
import javafx.util.Pair;

import java.util.*;

public class Room {
	
	private String roomName;
	private String roomPassword;
	private String roomOwner;
	private MessageCache<Integer, UserMessage> roomMessages;
	private Set<String> roomUsers;
	private Set<Integer> roomServers;

	public Room(final String paramName, final String paramPassword,final String paramOwner) {
		roomName = paramName;
		roomOwner = paramOwner;
		roomPassword = paramPassword;
		roomMessages = new MessageCache<>(100);
		roomUsers = new HashSet<>();
		roomServers = new HashSet<>();
	}

	public Room(final String roomName, final String roomOwner) {
		this(roomName, null, roomOwner);
	}

	public UserMessage[] getMessages() {
		return (UserMessage[]) roomMessages.getArray();
	}

	public int generateHash(final UserMessage paramMessage) {

		int hash = 7;

		hash = 37 * hash + (int) (paramMessage.getTimestamp() ^ (paramMessage.getTimestamp() >>> 32));
		hash = 37 * hash + Objects.hashCode(paramMessage.getSender());

		return hash;
	}

	public boolean insertMessage(final UserMessage paramMessage) {

		if (roomUsers.contains(paramMessage.getSender())) {

			int messageKey = generateHash(paramMessage);

			if (roomMessages.get(messageKey) != null) {
				return false;
			}

			roomMessages.add(messageKey, paramMessage);

			return true;
		}

		return false;
	}

    public boolean registerMirror(int serverId) {

        if (roomServers.contains(serverId)) {
            return false;
        }

        roomServers.add(serverId);

        return true;
    }

	public boolean removeMirror(int serverId) {

		if (!roomServers.contains(serverId)) {
			return false;
		}

		roomServers.remove(serverId);

		return true;
	}

	public boolean registerUser(final String userToken) {

		if (roomUsers.contains(userToken)) {
			return false;
		}

		roomUsers.add(userToken);

		return true;
	}

	public boolean removeUser(final String userToken)  {

		if (!roomUsers.contains(userToken)) {
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

	public final Set<String> getUsers() {
		return roomUsers;
	}

	public boolean hasUser(final String userToken) {
		return roomUsers.contains(userToken);
	}

    public Set<Integer> getServers() {
        return roomServers;
    }
}