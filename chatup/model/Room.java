package chatup.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

public class Room extends RoomInfo implements Serializable {

	private MessageCache<Integer, Message> roomMessages;

	public Room(final String paramName, final String paramPassword, final String paramOwner) {

        super(paramName, paramPassword, paramOwner);

		roomMessages = new MessageCache<>(100);
		lastSync = 0L;
	}

	private long lastSync;

	public Room(final String roomName, final String roomOwner) {
		this(roomName, null, roomOwner);
	}

	private int generateHash(final Message paramMessage) {

		int hash = 7;

		hash = 37 * hash + (int) (paramMessage.getTimestamp() ^ (paramMessage.getTimestamp() >>> 32));
		hash = 37 * hash + Objects.hashCode(paramMessage.getSender());

		return hash;
	}

	public boolean insertMessage(final Message paramMessage) {

		final String userToken = paramMessage.getSender();

		if (!roomUsers.contains(userToken)) {
			roomUsers.add(userToken);
		}

		int messageKey = generateHash(paramMessage);

		if (roomMessages.get(messageKey) != null) {
			return false;
		}

		roomMessages.add(messageKey, paramMessage);
        lastSync = Instant.now().getEpochSecond();

		return true;
	}

    @Override
    public boolean registerServer(int serverId) {

        if (roomServers.contains(serverId)) {
            return false;
        }

        roomServers.add(serverId);
        lastSync = Instant.now().getEpochSecond();

        return true;
    }

    @Override
	public boolean removeMirror(int serverId) {

		if (!roomServers.contains(serverId)) {
			return false;
		}

		roomServers.remove(serverId);
        lastSync = Instant.now().getEpochSecond();

		return true;
	}

    @Override
	public boolean registerUser(final String userToken) {

		if (roomUsers.contains(userToken)) {
			return false;
		}

		roomUsers.add(userToken);
        lastSync = Instant.now().getEpochSecond();

		return true;
	}

    @Override
    public boolean removeUser(final String userToken)  {

        if (roomUsers.contains(userToken)) {
            lastSync = Instant.now().getEpochSecond();
            roomUsers.remove(userToken);
        }
        else {
            return false;
        }

        return true;
    }

    public long getLastUpdate() {
        return lastSync;
    }

	public final MessageCache<Integer, Message> getMessages() {
		return roomMessages;
	}

    public void syncMessages(final MessageCache messageCache) {
        roomMessages.putAll(messageCache.getCache());
    }
}