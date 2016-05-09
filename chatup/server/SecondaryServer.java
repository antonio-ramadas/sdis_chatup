package chatup.server;

import chatup.backend.SecondaryDispatcher;
import chatup.rest.HttpRequest;
import chatup.room.Room;
import chatup.user.UserMessage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;

public class SecondaryServer extends Server {

	private final Database serverDatabase = Database.getInstance();

	public SecondaryServer(final ServerKeystore serverKeystore, short paramPort, short tcpPort) throws SQLException {
		super(serverKeystore, new SecondaryDispatcher(), paramPort, tcpPort);
	}

	public boolean sendHttpRequest(final HttpRequest myRequest) {

		final HttpURLConnection urlConnection;

		try {

			urlConnection = (HttpURLConnection) new URL("localhost:8080/roomServer").openConnection();
			urlConnection.setDoOutput(true);
			urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			urlConnection.setRequestMethod(myRequest.getMessage());

			try (final OutputStreamWriter os = new OutputStreamWriter(urlConnection.getOutputStream());
				 final BufferedWriter bw = new BufferedWriter(os)) {
				bw.write(URLEncoder.encode(myRequest.getMessage(), "UTF-8"));
				bw.close();
			}

			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
		catch (IOException ex) {
			return false;
		}

		return true;
	}

	@Override
	public boolean registerMessage(final String userToken, int roomId, final String messageBody) {

		if (!rooms.containsKey(roomId)) {
			return false;
		}

		final Room selectedRoom = rooms.get(roomId);

		if (!selectedRoom.hasUser(userToken)) {
			return false;
		}

		selectedRoom.registerMessage(userToken, roomId, messageBody);

		return true;
	}

	@Override
	public UserMessage[] retrieveMessages(final String userToken, int roomId) {

		if (!rooms.containsKey(roomId)) {
			return null;
		}

		final Room selectedRoom = rooms.get(roomId);

		if (!selectedRoom.hasUser(userToken)) {
			return null;
		}

		return selectedRoom.getMessages();
	}

	@Override
	public ServerType getType() {
		return ServerType.SECONDARY;
	}
}