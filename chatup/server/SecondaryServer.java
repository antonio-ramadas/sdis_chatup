package chatup.server;

import chatup.http.SecondaryDispatcher;
import chatup.http.HttpRequest;
import chatup.http.ServerResponse;
import chatup.model.Room;
import chatup.model.Message;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.time.Instant;

public class SecondaryServer extends Server {

	private final ServerInfo primaryServer;

	public SecondaryServer(final ServerKeystore serverKeystore, final ServerInfo paramPrimary, short httpPort, short tcpPort) throws SQLException {
		super(serverKeystore, new SecondaryDispatcher(), httpPort, tcpPort);
		primaryServer = paramPrimary;
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
	public ServerResponse registerMessage(final String userToken, int roomId, final String messageBody) {

		if (!rooms.containsKey(roomId)) {
			return ServerResponse.RoomNotFound;
		}

		final Room selectedRoom = rooms.get(roomId);

		if (!selectedRoom.hasUser(userToken)) {
			return ServerResponse.OperationFailed;
		}

		selectedRoom.insertMessage(new Message(roomId, userToken, Instant.now().toEpochMilli(), messageBody));

		return ServerResponse.SuccessResponse;
	}

	@Override
	public Message[] retrieveMessages(final String userToken, int roomId) {

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
	public ServerResponse insertServer(int serverId, String newIp, short newPort) {
		return ServerResponse.OperationFailed;
	}

	public ServerResponse updateServer(int serverId, final String newIp, short newPort) {

		final ServerInfo selectedServer = servers.get(serverId);

		if (selectedServer == null) {

			try {
				servers.put(serverId, new ServerInfo(newIp, newPort));
			}
			catch (UnknownHostException e) {
				return ServerResponse.OperationFailed;
			}
		}
		else {

			try {
				selectedServer.setAddress(InetAddress.getByName(newIp));
				selectedServer.setTcpPort(newPort);
			}
			catch (UnknownHostException ex) {
				return ServerResponse.OperationFailed;
			}
		}


		return ServerResponse.SuccessResponse;
	}

	public ServerResponse removeServer(int serverId) {

		final ServerInfo selectedServer = servers.get(serverId);

		if (selectedServer == null) {
			return ServerResponse.ServerNotFound;
		}

		servers.remove(serverId);

		return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse userDisconnect(final String userToken, final String userEmail) {

		System.out.println("email:" + userEmail);
		System.out.println("token:" + userToken);

		final String userRecord = users.get(userToken);

		if (userRecord == null || !userRecord.equals(userEmail)) {
			return ServerResponse.InvalidToken;
		}

		rooms.forEach((roomId, room) -> {

			if (room.hasUser(userToken)) {
				notifyLeaveRoom(roomId, userToken);
			}
		});

		users.remove(userToken);

		return ServerResponse.SuccessResponse;
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