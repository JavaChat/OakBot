package oakbot.command.weather;

import java.net.URI;

/**
 * A wttr.in response.
 * @param <T> the response body type
 * @param requestUri the request URI
 * @param content the response body
 * @author Michael Angstadt
 */
public record WttrInResponse<T>(URI requestUri, T content) {
}
