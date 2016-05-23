package chatup.model;

import java.time.Instant;
import java.util.LinkedList;

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
			roomMessages.push(paramMessage);
		}
		else {
			roomUsers.add(userToken);
			roomMessages.push(paramMessage);
		}

        lastSync = roomMessages.getLast().getTimestamp();

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
	public boolean removeServer(int serverId) {

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

    public void syncMessages(final Message[] messageCache) {

        for (final Message currentMessage : messageCache) {
            roomMessages.push(currentMessage);
        }

        lastSync = roomMessages.getLast().getTimestamp();
    }


    @Override
    public String toString() {
        return super.toString() + " #numberMessages=" + roomMessages.size();
    }

    public Message[] getMessages() {
        return roomMessages.getMessages(0);
    }

    public Message[] getMessages(long paramTimestamp) {
        return roomMessages.getMessages(paramTimestamp);
    }

    public void insertMessages(final LinkedList<Message> paramMessages) {

        for (final Message currentMessage : paramMessages) {
            roomMessages.push(currentMessage);
        }

        lastSync = roomMessages.getLast().getTimestamp();
    }
}