package oakbot.ai.openai;

import java.time.Instant;

/**
 * @author Michael Angstadt
 * @see "https://platform.openai.com/docs/api-reference/images/object"
 */
public class CreateImageResponse {
	private final Instant created;
	private final String url;
	private final byte[] data;
	private final String revisedPrompt;

	/**
	 * @param created the timestamp when the image was created
	 * @param url the URL to the image or null if not present
	 * @param data the image data or null if not present
	 * @param revisedPrompt the revised prompt or null if the user's prompt was
	 * used
	 */
	public CreateImageResponse(Instant created, String url, byte[] data, String revisedPrompt) {
		this.created = created;
		this.url = url;
		this.data = data;
		this.revisedPrompt = revisedPrompt;
	}

	/**
	 * Gets the timestamp that the image was created.
	 * @return the timestamp
	 */
	public Instant getCreated() {
		return created;
	}

	/**
	 * Gets the URL to the generated image. The URL is only valid for 1 hour.
	 * @return the URL to the image or null if not present
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Gets the image data of the generated image.
	 * @return the image data or null if not present
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Gets the prompt that was used to generate the image, if there was any
	 * revision to the prompt.
	 * @return the revised prompt or null if the user's prompt was used
	 */
	public String getRevisedPrompt() {
		return revisedPrompt;
	}
}
