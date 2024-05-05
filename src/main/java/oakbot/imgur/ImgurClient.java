package oakbot.imgur;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;

/**
 * @author Michael Angstadt
 * @see "https://apidocs.imgur.com"
 */
public class ImgurClient {
	private final String clientId;

	/**
	 * @param clientId the client ID
	 */
	public ImgurClient(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Uploads an image or video file.
	 * @param data the file data
	 * @return the URL of the uploaded file (e.g.
	 * "https://i.imgur.com/m0VmUir.jpeg")
	 * @throws ImgurException if an error response is returned
	 * @throws IOException if there's a network error
	 * @see <a href=
	 * "https://apidocs.imgur.com/#c85c9dfc-7487-4de2-9ecd-66f727cf3139">Support
	 * file types and API details</a>
	 */
	public String uploadFile(byte[] data) throws ImgurException, IOException {
		var request = postRequestWithClientId("/image");

		//@formatter:off
		request.setEntity(MultipartEntityBuilder.create()
			.addTextBody("type", "file")
			.addBinaryBody("image", data, ContentType.APPLICATION_OCTET_STREAM, "file")
		.build());
		//@formatter:on

		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				JsonNode responseBody;
				try (InputStream in = response.getEntity().getContent()) {
					responseBody = JsonUtils.parse(in);
				}

				lookForError(responseBody);

				return parseImageUploadResponse(responseBody);
			}
		}
	}

	private String parseImageUploadResponse(JsonNode node) {
		return node.path("data").path("link").asText();
	}

	/**
	 * Throws an exception if there is an error in the given response.
	 * @param response the response
	 * @throws ImgurException if there is an error in the given response
	 */
	private void lookForError(JsonNode response) throws ImgurException {
		var error = response.path("data").get("error");
		if (error == null) {
			return;
		}

		var message = error.asText();
		throw new ImgurException(message);
	}

	private HttpPost postRequestWithClientId(String uriPath) {
		var request = new HttpPost("https://api.imgur.com/3" + uriPath);
		request.setHeader("Authorization", "Client-ID " + clientId);
		return request;
	}
}
