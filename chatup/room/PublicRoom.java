package chatup.room;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PublicRoom extends Room
{
	public PublicRoom(int roomId, final String roomName) {
		super(roomId, roomName);
	}
	
	public PublicRoom(final ResultSet roomQuery) throws SQLException {
		super(roomQuery);
	}
}
