package chatup.room;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PrivateRoom extends Room {
	
	private String password;

	public PrivateRoom(int roomId, final String roomName) {
		super(roomId, roomName);
	}
	
	public PrivateRoom(final ResultSet roomQuery) throws SQLException {
		super(roomQuery);
	}
}
