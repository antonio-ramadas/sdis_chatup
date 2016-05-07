package chatup.room;

import chatup.user.UserSession;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Room {
	
	private String roomName;
	private String roomPassword;

	private HashMap<String, String> users;
	private Set<Integer> servers;

	public Room(final String roomName, final String roomPassword) {
		this.roomName = roomName;
		this.roomPassword = roomPassword;
		this.users = new HashMap<>();
		this.servers = new HashSet<>();
	}

	public Room(final String roomName) {
		this(roomName, null);
	}

	public final void registerUser(final UserSession paramUser) {
	}

	public final void removeUser(final String userToken) {

		if (users.containsKey(userToken)) {
			users.remove(userToken);
		}
	}

	public final boolean isPrivate() {
		return roomPassword != null;
	}

	public final String getName() {
		return roomName;
	}

	public final String getPassword() {
		return roomPassword;
	}
}