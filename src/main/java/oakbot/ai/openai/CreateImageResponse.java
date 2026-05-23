package oakbot.ai.openai;

import java.time.Instant;

/**
 * @param created the timestamp when the image was created
 * @param url the URL to the image or null if not present
 * @param data the image data or null if not present
 * @param revisedPrompt the revised prompt or null if the prompt was not revised
 * @author Michael Angstadt
 * @see "https://developers.openai.com/api/reference/resources/images/methods/generate#(resource)%20images%20%3E%20(model)%20images_response%20%3E%20(schema)"
 */
public record CreateImageResponse(Instant created, String url, byte[] data, String revisedPrompt) {
}
