package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.model.SimplePair;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.esotericsoftware.minlog.Log;

import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FacebookService {

    private final String serviceUrl;
    private final String userToken;
    private final HttpExchange httpExchange;

    public FacebookService(final HttpExchange paramExchange, final String paramToken) throws MalformedURLException {

        final SimplePair[] simplePair = new SimplePair[] {
            new SimplePair("fields", "name,email"),
            new SimplePair("access_token", paramToken)
        };

        httpExchange = paramExchange;
        userToken = paramToken;
        serviceUrl = "https://graph.facebook.com/v2.3/me" + new HttpQuery(simplePair).toString();
    }

    private ServerResponse httpError;

    public boolean validateToken() {

        final HttpURLConnection httpConnection;

        try {
            httpConnection = (HttpURLConnection) new URL(serviceUrl).openConnection();
        }
        catch (final IOException ignored) {
            return false;
        }

        boolean exceptionThrown = false;

        try {
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Accept", ChatupGlobals.JsonType);
            httpConnection.setRequestProperty("User-Agent", ChatupGlobals.UserAgent);
            httpConnection.getResponseCode();
        }
        catch (final IOException ex) {
            exceptionThrown = true;
            httpError = ServerResponse.ServiceOffline;
        }

        if (exceptionThrown) {
            sendError(httpError);
        }
        else {

            final StringBuilder response = new StringBuilder();

            try (final BufferedReader br = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()))) {

                for (String inputLine = br.readLine(); inputLine != null; inputLine = br.readLine()) {
                    response.append(inputLine);
                }
            }
            catch (final IOException ex) {
                exceptionThrown = true;
                httpError = ServerResponse.ServiceOffline;
            }

            if (exceptionThrown) {
                sendError(httpError);
            }
            else {

                final String responseString = response.toString();

                if (responseString.isEmpty()) {
                    sendError(ServerResponse.MissingParameters);
                }
                else {

                    final JsonValue jsonValue = Json.parse(responseString);

                    if (jsonValue.isObject()) {

                        final JsonObject jsonObject = jsonValue.asObject();
                        final String userEmail = jsonObject.getString("email", null);

                        if (userEmail == null || userEmail.isEmpty()) {
                            sendError(ServerResponse.AuthenticationFailed);
                        }
                        else {
                            sendJsonResponse(ServerResponse.SuccessResponse, Json.object()
                                    .add(HttpFields.UserToken, userToken)
                                    .add(HttpFields.UserEmail, userEmail)
                            );
                        }
                    }
                    else {
                        sendError(ServerResponse.AuthenticationFailed);
                    }
                }
            }
        }

        return !exceptionThrown;
    }

    private void sendJsonResponse(final ServerResponse httpResponse, final JsonValue httpParameters) {

        if (httpResponse == ServerResponse.SuccessResponse) {

            try {
                sendResponse(
                    Json.object().add(HttpCommands.GenericSuccess, httpParameters).toString(),
                    HttpURLConnection.HTTP_OK
                );
            }
            catch (final IOException ex) {
                ChatupGlobals.warn("FacebookService", ex);
            }
        }
        else {
            sendError(httpResponse);
        }
    }

    private void sendError(final ServerResponse serverResponse) {

        try {
            sendResponse(
                Json.object().add(HttpCommands.GenericError, serverResponse.toString()).toString(),
                HttpURLConnection.HTTP_OK
            );
        }
        catch (final IOException ex) {
            ChatupGlobals.warn("FacebookService", ex);
        }
    }

    private void sendResponse(final String serverResponse, int statusCode) throws IOException {

        Log.info("[FacebookService] sending response: " + serverResponse);
        httpExchange.sendResponseHeaders(statusCode, serverResponse.length());

        try (final OutputStream os = httpExchange.getResponseBody()) {
            os.write(serverResponse.getBytes());
            os.close();
        }
        catch (final IOException ex) {
            ChatupGlobals.warn(ChatupGlobals.MessageServiceUrl, ex);
        }
    }
}