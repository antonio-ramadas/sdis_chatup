package chatup.server;

import chatup.http.SecondaryDispatcher;
import chatup.http.HttpRequest;
import chatup.room.Room;
import chatup.user.UserMessage;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.time.Instant;

public class SecondaryServer extends Server {

	private final Database serverDatabase = Database.getInstance();

	public SecondaryServer(final ServerKeystore serverKeystore, short httpPort, short tcpPort) throws SQLException {
		super(serverKeystore, new SecondaryDispatcher(), httpPort, tcpPort);
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

		selectedRoom.insertMessage(new UserMessage(roomId, userToken, Instant.now().toEpochMilli(), messageBody));

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
	public boolean insertServer(int serverId, String newIp, short newPort) {
		return false;
	}

	public boolean updateServer(int serverId, final String newIp, short newPort) {

		final ServerInfo selectedServer = servers.get(serverId);

		if (selectedServer == null) {

			try {
				servers.put(serverId, new ServerInfo(newIp, newPort));
			}
			catch (UnknownHostException e) {
				return false;
			}
		}
		else {

			try {
				selectedServer.setAddress(InetAddress.getByName(newIp));
				selectedServer.setTcpPort(newPort);
			}
			catch (UnknownHostException ex) {
				return false;
			}
		}


		return true;
	}

	public boolean removeServer(int serverId) {

		final ServerInfo selectedServer = servers.get(serverId);

		if (selectedServer == null) {
			return false;
		}

		servers.remove(serverId);

		return true;
	}

	@Override
	public boolean userDisconnect(final String userToken, final String userEmail) {

		System.out.println("email:" + userEmail);
		System.out.println("token:" + userToken);

		final String userRecord = users.get(userToken);

		if (userRecord == null || !userRecord.equals(userEmail)) {
			return false;
		}

		rooms.forEach((roomId, room) -> {

			if (room.hasUser(userToken)) {
				notifyLeaveRoom(roomId, userToken);
			}
		});

		users.remove(userToken);

		return true;
	}

	@Override
	protected void notifyLeaveRoom(int roomId, final String userToken) {
		throw new UnsupportedOperationException("NotifyLeaveRoom");
	}

	@Override
	public ServerType getType() {
		return ServerType.SECONDARY;
	}
}