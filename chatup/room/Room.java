package chatup.room;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public abstract class Room {
	
	private String roomName;
	private Set<String> users;
	private Set<Integer> servers;
	
	protected Room(int roomId, final String roomName) {
		this.roomName = roomName;
	}
	
	protected Room(final ResultSet roomQuery) throws SQLException {	
		this.roomName = roomQuery.getString("name");
	}

	public final String getName() {
		return roomName;
	}
}