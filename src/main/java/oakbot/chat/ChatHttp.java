package oakbot.chat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import oakbot.util.Http;
import oakbot.util.JsonUtils;
import oakbot.util.Sleeper;

/**
 * Helper class for sending HTTP requests to the StackOverflow chat room.
 * In particular, this class handles HTTP 409 responses automatically when the
 * client sends too many messages too quickly.
 * @author Michael Angstadt
 */
public class ChatHttp extends Http {
	private static final Logger logger = Logger.getLogger(ChatHttp.class.getName());
	private static final Pattern response409Regex = Pattern.compile("\\d+");

	/**
	 * @param client the HTTP client object to wrap
	 */
	public ChatHttp(CloseableHttpClient client) {
		super(client);
	}

	/**
	 * <p>
	 * Sends an HTTP request.
	 * </p>
	 * <p>
	 * The chat system returns an HTTP 409 response if the client sends too many
	 * requests too quickly. This method automatically handles such responses by
	 * sleeping the requested amount of time, and then re-sending the request.
	 * It will do this up to five times before giving up, at which point an
	 * {@code IOException} will be thrown.
	 * </p>
	 * @param request the request to send
	 * @return the response
	 * @throws IOException if there was a problem sending the request
	 */
	@Override
	protected Response send(HttpUriRequest request) throws IOException {
		long sleep = 0;
		int attempts = 0;

		while (attempts < 5) {
			if (sleep > 0) {
				logger.info("Sleeping for " + sleep + "ms before resending the request...");
				Sleeper.sleep(sleep);
			}

			Response response = super.send(request);

			/*
			 * An HTTP 409 response means that the bot is sending messages too
			 * quickly. The response body contains the number of seconds the bot
			 * must wait before it can post another message.
			 */
			if (response.getStatusCode() == 409) {
				String body = response.getBody();
				Long waitTime = parse409Response(body);
				sleep = (waitTime == null) ? 5000 : waitTime;

				logger.info("HTTP " + response.getStatusCode() + " response, sleeping " + sleep + "ms [request-method=" + request.getMethod() + "; request-URI=" + request.getURI() + "]: " + body);

				attempts++;
				continue;
			}

			if (logger.isLoggable(Level.FINE)) {
				String bodyDebug;
				try {
					JsonNode node = response.getBodyAsJson();
					bodyDebug = JsonUtils.prettyPrint(node);
				} catch (JsonProcessingException e) {
					//not JSON
					bodyDebug = response.getBody();
				}
				logger.fine("Received response [status=" + response.getStatusCode() + "; request-method=" + request.getMethod() + "; request-URI=" + request.getURI() + "]: " + bodyDebug);
			}

			return response;
		}

		throw new IOException("Request could not be sent after " + attempts + " attempts [request-method=" + request.getMethod() + "; request-URI=" + request.getURI() + "].");
	}

	/**
	 * Parses the wait time out of an HTTP 409 response. An HTTP 409 response
	 * indicates that the bot is sending messages too quickly.
	 * @param body the response body (e.g. "You can perform this action again in
	 * 2 seconds")
	 * @return the amount of time (in milliseconds) the bot must wait before the
	 * chat system will accept the request, or null if this value could not be
	 * parsed from the response body
	 */
	private Long parse409Response(String body) {
		Matcher m = response409Regex.matcher(body);
		if (!m.find()) {
			return null;
		}

		int seconds = Integer.parseInt(m.group(0));
		return TimeUnit.SECONDS.toMillis(seconds);
	}
}
