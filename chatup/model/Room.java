package chatup.model;

import java.time.Instant;

public class Room extends RoomInfo {

	private MessageCache roomMessages;

	public Room(final String paramName, final String paramPassword, final String paramOwner) {

        super(paramName, paramPassword, paramOwner);

		roomMessages = new MessageCache();
		lastSync = 0L;
	}

	private long lastSync;

	public boolean insertMessage(final Message paramMessage) {

		final String userToken = paramMessage.getSender();

		if (roomUsers.contains(userToken)) {
			roomMessages.add(paramMessage);
		}
		else {
			roomUsers.add(userToken);
			roomMessages.add(paramMessage);
		}

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

	public final MessageCache getMessages() {
		return roomMessages;
	}

    public void syncMessages(final MessageCache messageCache) {
        roomMessages.putAll(messageCache.getCache());
    }
}