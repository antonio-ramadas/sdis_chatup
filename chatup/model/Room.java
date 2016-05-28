package chatup.model;

import java.time.Instant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Room {

    private String mName;
    private String mPassword;
    private String mOwner;
    private MessageCache mMessages;
    private Set<String> mUsers;
    private Set<Integer> mServers;

    public Room(final String paramName, final String paramPassword, final String paramOwner) {
        this(paramName, paramPassword, Instant.now().getEpochSecond(), paramOwner);
	}

    public Room(final String paramName, final String paramPassword, long paramTimestamp, final String paramOwner) {
        mName = paramName;
        mOwner = paramOwner;
        mPassword = paramPassword;
        mMessages = new MessageCache();
        mUsers = new HashSet<>();
        mServers = new HashSet<>();
        mTimestamp = paramTimestamp;
    }

	private long mTimestamp;

    public boolean updateUsers(final Set<String> paramUsers) {
        return mUsers.addAll(paramUsers);
    }

    public Set<Integer> updateServers(final Set<Integer> paramServers) {

        final Set<Integer> newServers = new HashSet<>();

        for (final Integer serverId : paramServers) {

            if (mServers.add(serverId)) {
                newServers.add(serverId);
            }
        }

        return newServers;
    }

	public boolean insertMessage(final Message paramMessage) {

		final String userToken = paramMessage.getToken();

		if (mUsers.contains(userToken)) {
			mMessages.push(paramMessage);
		}
		else {
			mUsers.add(userToken);
			mMessages.push(paramMessage);
		}

        mTimestamp = mMessages.getLast().getTimestamp();

		return true;
	}

    public boolean registerServer(int serverId) {

        if (mServers.contains(serverId)) {
            return false;
        }

        mServers.add(serverId);
        mTimestamp = Instant.now().getEpochSecond();

        return true;
    }

	public boolean removeServer(int serverId) {

		if (!mServers.contains(serverId)) {
			return false;
		}

		mServers.remove(serverId);
        mTimestamp = Instant.now().getEpochSecond();

		return true;
	}

	public boolean registerUser(final String userToken) {

		if (mUsers.contains(userToken)) {
			return false;
		}

		mUsers.add(userToken);
        mTimestamp = Instant.now().getEpochSecond();

		return true;
	}

    public boolean removeUser(final String userToken)  {

        if (mUsers.contains(userToken)) {
            mTimestamp = Instant.now().getEpochSecond();
            mUsers.remove(userToken);
        }
        else {
            return false;
        }

        return true;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setServers(final Set<Integer> paramServers) {
        mServers = paramServers;
    }

    @Override
    public String toString() {
        return mName + " " + mServers + " #numberMessages=" + mMessages.size();
    }

    public final String getName() {
        return mName;
    }

    public final String getOwner() {
        return mOwner;
    }

    public final String getPassword() {
        return mPassword;
    }

    public final Set<Integer> getServers() {
        return mServers;
    }

    public final Set<String> getUsers() {
        return mUsers;
    }

    public ArrayList<Message> getMessages(long paramTimestamp) {
        return mMessages.getMessages(paramTimestamp);
    }

    public boolean hasUser(final String userToken) {
        return mUsers.contains(userToken);
    }

    public boolean isEmpty() {
        return mUsers.isEmpty();
    }

    public boolean isPrivate() {
        return mPassword != null;
    }

    public void insertMessages(final MessageCache paramMessages) {
        mMessages = paramMessages;
        mTimestamp = mMessages.getLast().getTimestamp();
    }
}