package oakbot.ai.stabilityai;

/**
 * @author Michael Angstadt
 */
public class StableImageResponse {
	private final byte[] image;
	private final String contentType;
	private final String finishReason;
	private final int seed;

	public StableImageResponse(byte[] image, String contentType, String finishReason, int seed) {
		this.image = image;
		this.contentType = contentType;
		this.finishReason = finishReason;
		this.seed = seed;
	}

	public byte[] getImage() {
		return image;
	}

	public String getContentType() {
		return contentType;
	}

	public String getFinishReason() {
		return finishReason;
	}

	public int getSeed() {
		return seed;
	}
}
