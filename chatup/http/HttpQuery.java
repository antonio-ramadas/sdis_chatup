package chatup.http;

import chatup.model.SimplePair;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HttpQuery {

    private final StringBuilder sb = new StringBuilder();

    public HttpQuery(final SimplePair[] httpParameters) {

        if (httpParameters.length > 0) {

            sb.append("?");

            for (int i = 0; i < httpParameters.length; i++) {

                try {
                    sb.append(httpParameters[i].getFirst()).append("=").append(URLEncoder.encode(httpParameters[i].getSecond(), StandardCharsets.UTF_8.toString()));
                }
                catch (final Throwable ex) {
                    break;
                }

                if (i < httpParameters.length - 1) {
                    sb.append("&");
                }
            }
        }
    }

    @Override
    public final String toString() {
        return sb.toString();
    }
}