package chatup.model;

import java.util.*;

public class Room {
	
	private String roomName;
	private String roomPassword;
	private String roomOwner;
	private MessageCache<Integer, Message> roomMessages;
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

	public Message[] getMessages() {
		return (Message[]) roomMessages.getArray();
	}

	public int generateHash(final Message paramMessage) {

		int hash = 7;

		hash = 37 * hash + (int) (paramMessage.getTimestamp() ^ (paramMessage.getTimestamp() >>> 32));
		hash = 37 * hash + Objects.hashCode(paramMessage.getSender());

		return hash;
	}

	public boolean insertMessage(final Message paramMessage) {

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
		return roomUsers.isEmpty();
	}

	public boolean isPrivate() {
		return roomPassword != null;
	}

	public boolean hasUser(final String userToken) {
		return roomUsers.contains(userToken);
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

    public final Set<Integer> getServers() {
        return roomServers;
    }
}